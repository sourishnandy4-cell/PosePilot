package com.posepilot.app.ui.camera

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.SurfaceRequest
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posepilot.app.data.camera.CameraManager
import com.posepilot.app.data.pose.PoseLandmarkerHelper
import com.posepilot.app.data.repository.GalleryRepository
import com.posepilot.app.data.sensor.RotationSensorListener
import com.posepilot.app.data.audio.AudioCoachingManager
import com.posepilot.app.data.db.SessionRecord
import com.posepilot.app.data.db.SessionRecordDao
import com.posepilot.app.data.repository.SettingsRepository
import com.posepilot.app.domain.analysis.PoseSimilarityCalculator
import com.posepilot.app.domain.analysis.PostureAnalysisEngine
import com.posepilot.app.domain.model.IssueType
import com.posepilot.app.domain.model.PoseLandmarkPoint
import com.posepilot.app.domain.model.PoseTemplate
import com.posepilot.app.domain.model.PostureAnalysisResult
import com.posepilot.app.domain.model.PostureIssue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    private val galleryRepository: GalleryRepository,
    private val poseLandmarkerHelper: PoseLandmarkerHelper,
    private val rotationSensorListener: RotationSensorListener,
    private val settingsRepository: SettingsRepository,
    private val audioCoachingManager: AudioCoachingManager,
    private val sessionRecordDao: SessionRecordDao
) : ViewModel() {

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    val surfaceRequest: StateFlow<SurfaceRequest?> = cameraManager.surfaceRequest
    val poseResults: StateFlow<List<PoseLandmarkPoint>?> = poseLandmarkerHelper.poseResults

    private val _postureAnalysis = MutableStateFlow<PostureAnalysisResult?>(null)
    val postureAnalysis: StateFlow<PostureAnalysisResult?> = _postureAnalysis.asStateFlow()

    private val _coachingIssue = MutableStateFlow<PostureIssue?>(null)
    val coachingIssue: StateFlow<PostureIssue?> = _coachingIssue.asStateFlow()

    private val _lastCapturedUri = MutableStateFlow<Uri?>(null)
    val lastCapturedUri: StateFlow<Uri?> = _lastCapturedUri.asStateFlow()

    val deviceRoll: StateFlow<Float> = rotationSensorListener.deviceRoll

    val showGrid: StateFlow<Boolean> = settingsRepository.showGrid
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showHorizon: StateFlow<Boolean> = settingsRepository.showHorizon
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val audioEnabled: StateFlow<Boolean> = settingsRepository.audioEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val sensitivity: StateFlow<Float> = settingsRepository.sensitivity
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    val smartShutterEnabled: StateFlow<Boolean> = settingsRepository.smartShutterEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val smartShutterDuration: StateFlow<Int> = settingsRepository.smartShutterDuration
        .stateIn(viewModelScope, SharingStarted.Eagerly, 3)

    private val _countdownValue = MutableStateFlow<Int?>(null)
    val countdownValue: StateFlow<Int?> = _countdownValue.asStateFlow()

    private val _activeTemplate = MutableStateFlow<PoseTemplate?>(null)
    val activeTemplate: StateFlow<PoseTemplate?> = _activeTemplate.asStateFlow()

    private val _poseSimilarityScore = MutableStateFlow<Int?>(null)
    val poseSimilarityScore: StateFlow<Int?> = _poseSimilarityScore.asStateFlow()

    private var lastIssueType: IssueType? = null
    private var issueTypeStartTime = 0L
    private val debouncedDelayMs = 500L

    private var lastSpokenMessage: String? = null
    private var lastSpokenTime = 0L
    private val speechThrottleMs = 3000L
    private var hasSpokenReady = false

    private val sessionCorrectionsSet = mutableSetOf<String>()

    private var countdownJob: kotlinx.coroutines.Job? = null
    private var isCountingDown = false
    private var holdStartTime = 0L
    private val holdRequiredMs = 1000L

    init {
        loadLastCapturedPhoto()
        observePoseResults()
    }

    fun selectTemplate(template: PoseTemplate?) {
        _activeTemplate.value = template
        val landmarks = poseResults.value
        if (landmarks != null && template != null) {
            _poseSimilarityScore.value = PoseSimilarityCalculator.calculate(landmarks, template)
        } else {
            _poseSimilarityScore.value = null
        }
    }

    fun toggleGrid() {
        viewModelScope.launch {
            settingsRepository.updateShowGrid(!showGrid.value)
        }
    }

    fun toggleHorizon() {
        viewModelScope.launch {
            settingsRepository.updateShowHorizon(!showHorizon.value)
        }
    }

    fun updateSensitivity(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateSensitivity(value)
        }
    }

    fun toggleAudio() {
        viewModelScope.launch {
            settingsRepository.updateAudioEnabled(!audioEnabled.value)
        }
    }

    fun toggleSmartShutter() {
        viewModelScope.launch {
            settingsRepository.updateSmartShutterEnabled(!smartShutterEnabled.value)
        }
    }

    fun updateSmartShutterDuration(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateSmartShutterDuration(value)
        }
    }

    private fun startCountdown() {
        isCountingDown = true
        countdownJob = viewModelScope.launch {
            var count = smartShutterDuration.value
            while (count > 0) {
                _countdownValue.value = count
                if (audioEnabled.value) {
                    audioCoachingManager.speak(count.toString())
                }
                kotlinx.coroutines.delay(1000)
                count--
            }
            _countdownValue.value = null
            isCountingDown = false
            countdownJob = null

            if (audioEnabled.value) {
                audioCoachingManager.speak("Cheese!")
            }

            takePhoto(
                onPhotoSaved = {},
                onError = { it.printStackTrace() }
            )
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _countdownValue.value = null
        isCountingDown = false
        holdStartTime = 0L
        if (audioEnabled.value) {
            audioCoachingManager.speak("Reset")
        }
    }

    private fun observePoseResults() {
        viewModelScope.launch {
            poseResults.collect { landmarks ->
                if (landmarks == null) {
                    _postureAnalysis.value = null
                    _coachingIssue.value = null
                    _poseSimilarityScore.value = null
                    lastIssueType = null
                    if (isCountingDown) cancelCountdown()
                    return@collect
                }

                // Analyze pose with current persistent sensitivity multiplier
                val analysis = PostureAnalysisEngine(sensitivity.value).analyze(landmarks)
                _postureAnalysis.value = analysis

                val template = _activeTemplate.value
                if (template != null) {
                    _poseSimilarityScore.value = PoseSimilarityCalculator.calculate(landmarks, template)
                } else {
                    _poseSimilarityScore.value = null
                }

                val currentIssue = analysis.issues.firstOrNull()
                val currentTime = System.currentTimeMillis()

                if (currentIssue == null) {
                    _coachingIssue.value = null
                    lastIssueType = null
                    
                    // Trigger success cue exactly once when aligned
                    if (analysis.overallScore >= 80) {
                        if (audioEnabled.value && !hasSpokenReady) {
                            audioCoachingManager.speak("Looking good, hold still!")
                            hasSpokenReady = true
                            lastSpokenMessage = null
                        }
                    }
                } else {
                    if (currentIssue.type == lastIssueType) {
                        if (currentTime - issueTypeStartTime >= debouncedDelayMs) {
                            _coachingIssue.value = currentIssue
                        }
                    } else {
                        lastIssueType = currentIssue.type
                        issueTypeStartTime = currentTime

                        if (_coachingIssue.value == null) {
                            _coachingIssue.value = currentIssue
                        }
                    }
                }

                // Monitor posture for Smart Shutter triggers
                val isReady = analysis.overallScore >= 80 && analysis.issues.isEmpty()
                if (smartShutterEnabled.value) {
                    if (isReady) {
                        if (!isCountingDown && countdownJob == null) {
                            if (holdStartTime == 0L) {
                                holdStartTime = currentTime
                            } else if (currentTime - holdStartTime >= holdRequiredMs) {
                                startCountdown()
                            }
                        }
                    } else {
                        holdStartTime = 0L
                        if (analysis.overallScore < 65 && isCountingDown) {
                            cancelCountdown()
                        }
                    }
                }

                // Reset hasSpokenReady if posture quality deteriorates
                if (analysis.overallScore < 65) {
                    hasSpokenReady = false
                }

                // Handle vocal instructions with 3-second throttle
                val activeIssue = _coachingIssue.value
                if (audioEnabled.value && activeIssue != null) {
                    val msg = activeIssue.message
                    if (msg != lastSpokenMessage || currentTime - lastSpokenTime >= speechThrottleMs) {
                        audioCoachingManager.speak(msg)
                        lastSpokenMessage = msg
                        lastSpokenTime = currentTime
                        hasSpokenReady = false
                        sessionCorrectionsSet.add(msg)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioCoachingManager.shutdown()
    }

    private fun loadLastCapturedPhoto() {
        viewModelScope.launch {
            val photos = galleryRepository.getPhotos()
            if (photos.isNotEmpty()) {
                _lastCapturedUri.value = photos.first()
            }
        }
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            cameraManager.bindCamera(lifecycleOwner, _lensFacing.value)
        }
        rotationSensorListener.startListening()
    }

    fun unbindCamera() {
        cameraManager.unbindCamera()
        rotationSensorListener.stopListening()
    }

    fun toggleCamera(lifecycleOwner: LifecycleOwner) {
        _lensFacing.value = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCamera(lifecycleOwner)
    }

    fun takePhoto(onPhotoSaved: (Uri) -> Unit, onError: (Exception) -> Unit) {
        cameraManager.takePhoto(
            onSuccess = { uri ->
                _lastCapturedUri.value = uri
                onPhotoSaved(uri)

                val score = postureAnalysis.value?.overallScore ?: 0
                val templateId = activeTemplate.value?.id
                val correctionCount = sessionCorrectionsSet.size

                viewModelScope.launch {
                    sessionRecordDao.insertRecord(
                        SessionRecord(
                            timestampMs = System.currentTimeMillis(),
                            averageScore = score,
                            poseTemplateId = templateId,
                            correctionCount = correctionCount
                        )
                    )
                    sessionCorrectionsSet.clear()
                }
            },
            onError = { exception ->
                onError(exception)
            }
        )
    }
}
