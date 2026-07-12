package com.posepilot.app.ui.camera

import androidx.camera.core.CameraSelector
import androidx.lifecycle.LifecycleOwner
import com.posepilot.app.data.camera.CameraManager
import com.posepilot.app.data.pose.PoseLandmarkerHelper
import com.posepilot.app.data.audio.AudioCoachingManager
import com.posepilot.app.data.db.SessionRecordDao
import com.posepilot.app.data.repository.GalleryRepository
import com.posepilot.app.data.repository.SettingsRepository
import com.posepilot.app.data.sensor.RotationSensorListener
import com.posepilot.app.domain.model.PoseLandmarkPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var cameraManager: CameraManager
    private lateinit var galleryRepository: GalleryRepository
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private lateinit var rotationSensorListener: RotationSensorListener
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var audioCoachingManager: AudioCoachingManager
    private lateinit var sessionRecordDao: SessionRecordDao
    private lateinit var viewModel: CameraViewModel

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)
        cameraManager = mock(CameraManager::class.java)
        galleryRepository = mock(GalleryRepository::class.java)
        poseLandmarkerHelper = mock(PoseLandmarkerHelper::class.java)
        rotationSensorListener = mock(RotationSensorListener::class.java)
        settingsRepository = mock(SettingsRepository::class.java)
        audioCoachingManager = mock(AudioCoachingManager::class.java)
        sessionRecordDao = mock(SessionRecordDao::class.java)
        
        `when`(galleryRepository.getPhotos()).thenReturn(emptyList())
        `when`(poseLandmarkerHelper.poseResults).thenReturn(MutableStateFlow(null))
        `when`(rotationSensorListener.deviceRoll).thenReturn(MutableStateFlow(0f))
        
        `when`(settingsRepository.showGrid).thenReturn(MutableStateFlow(true))
        `when`(settingsRepository.showHorizon).thenReturn(MutableStateFlow(true))
        `when`(settingsRepository.audioEnabled).thenReturn(MutableStateFlow(true))
        `when`(settingsRepository.sensitivity).thenReturn(MutableStateFlow(1.0f))
        `when`(settingsRepository.smartShutterEnabled).thenReturn(MutableStateFlow(true))
        `when`(settingsRepository.smartShutterDuration).thenReturn(MutableStateFlow(3))
        
        viewModel = CameraViewModel(
            cameraManager,
            galleryRepository,
            poseLandmarkerHelper,
            rotationSensorListener,
            settingsRepository,
            audioCoachingManager,
            sessionRecordDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLensFacingIsBack() = runTest {
        assertEquals(CameraSelector.LENS_FACING_BACK, viewModel.lensFacing.value)
    }

    @Test
    fun testToggleCameraSwitchesLens() = runTest {
        val mockLifecycleOwner = mock(LifecycleOwner::class.java)
        
        // Initial state is back camera
        assertEquals(CameraSelector.LENS_FACING_BACK, viewModel.lensFacing.value)

        // Toggle camera
        viewModel.toggleCamera(mockLifecycleOwner)

        // Check it switched to front
        assertEquals(CameraSelector.LENS_FACING_FRONT, viewModel.lensFacing.value)

        // Toggle again
        viewModel.toggleCamera(mockLifecycleOwner)

        // Check it switched back to back
        assertEquals(CameraSelector.LENS_FACING_BACK, viewModel.lensFacing.value)
    }

    @Test
    fun testPostureAnalysisFlowUpdates() = runTest {
        val mockPoints = listOf(
            PoseLandmarkPoint(com.posepilot.app.domain.model.LandmarkType.NOSE, 0.33f, 0.33f, 0f, 0.9f)
        )
        val resultsFlow = viewModel.poseResults as MutableStateFlow
        resultsFlow.value = mockPoints

        testDispatcher.scheduler.advanceUntilIdle()

        org.junit.Assert.assertNotNull(viewModel.postureAnalysis.value)
    }
}
