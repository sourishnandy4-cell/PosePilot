package com.posepilot.app.data.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.posepilot.app.domain.model.LandmarkType
import com.posepilot.app.domain.model.PoseLandmarkPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoseLandmarkerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var poseLandmarker: PoseLandmarker? = null

    private val _poseResults = MutableStateFlow<List<PoseLandmarkPoint>?>(null)
    val poseResults: StateFlow<List<PoseLandmarkPoint>?> = _poseResults.asStateFlow()

    private var isInitializing = false

    private var cachedBitmapBuffer: Bitmap? = null
    private var cachedRotatedBitmap: Bitmap? = null
    private var canvas: android.graphics.Canvas? = null

    init {
        setupPoseLandmarker()
    }

    private fun setupPoseLandmarker() {
        if (poseLandmarker != null || isInitializing) return
        isInitializing = true

        Thread {
            try {
                val baseOptionsBuilder = BaseOptions.builder()
                    .setModelAssetPath("pose_landmarker_lite.task")

                val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptionsBuilder.build())
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener { result, _ ->
                        processInferenceResult(result)
                    }
                    .setErrorListener { error ->
                        error.printStackTrace()
                    }

                poseLandmarker = PoseLandmarker.createFromOptions(context, optionsBuilder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isInitializing = false
            }
        }.start()
    }

    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val landmarker = poseLandmarker ?: run {
            setupPoseLandmarker()
            imageProxy.close()
            return
        }

        val frameTime = SystemClock.uptimeMillis()

        try {
            val width = imageProxy.width
            val height = imageProxy.height

            var bitmapBuffer = cachedBitmapBuffer
            if (bitmapBuffer == null || bitmapBuffer.width != width || bitmapBuffer.height != height) {
                bitmapBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                cachedBitmapBuffer = bitmapBuffer
            }

            imageProxy.use {
                bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
            }

            val rotation = imageProxy.imageInfo.rotationDegrees
            val rotatedWidth = if (rotation == 90 || rotation == 270) height else width
            val rotatedHeight = if (rotation == 90 || rotation == 270) width else height

            var rotatedBitmap = cachedRotatedBitmap
            var drawCanvas = canvas
            if (rotatedBitmap == null || rotatedBitmap.width != rotatedWidth || rotatedBitmap.height != rotatedHeight) {
                rotatedBitmap = Bitmap.createBitmap(rotatedWidth, rotatedHeight, Bitmap.Config.ARGB_8888)
                cachedRotatedBitmap = rotatedBitmap
                drawCanvas = android.graphics.Canvas(rotatedBitmap)
                canvas = drawCanvas
            }

            val matrix = Matrix().apply {
                postRotate(rotation.toFloat())
                if (isFrontCamera) {
                    postScale(-1f, 1f, width.toFloat() / 2f, height.toFloat() / 2f)
                }
                if (rotation == 90) {
                    postTranslate(height.toFloat(), 0f)
                } else if (rotation == 180) {
                    postTranslate(width.toFloat(), height.toFloat())
                } else if (rotation == 270) {
                    postTranslate(0f, width.toFloat())
                }
            }

            drawCanvas?.drawBitmap(bitmapBuffer, matrix, null)

            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            landmarker.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processInferenceResult(result: PoseLandmarkerResult) {
        val domainLandmarks = mutableListOf<PoseLandmarkPoint>()
        if (result.landmarks().isNotEmpty()) {
            val firstPerson = result.landmarks()[0]
            firstPerson.forEachIndexed { index, normalizedLandmark ->
                val type = LandmarkType.fromIndex(index)
                if (type != null) {
                    domainLandmarks.add(
                        PoseLandmarkPoint(
                            type = type,
                            x = normalizedLandmark.x(),
                            y = normalizedLandmark.y(),
                            z = normalizedLandmark.z(),
                            visibility = normalizedLandmark.visibility().orElse(0f)
                        )
                    )
                }
            }
            _poseResults.value = domainLandmarks
        } else {
            _poseResults.value = emptyList()
        }
    }

    fun clear() {
        poseLandmarker?.close()
        poseLandmarker = null
        _poseResults.value = null

        cachedBitmapBuffer?.recycle()
        cachedBitmapBuffer = null
        cachedRotatedBitmap?.recycle()
        cachedRotatedBitmap = null
        canvas = null
    }
}
