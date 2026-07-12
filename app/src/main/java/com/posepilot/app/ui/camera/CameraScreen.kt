package com.posepilot.app.ui.camera

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.abs
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.posepilot.app.domain.model.IssueType
import com.posepilot.app.domain.model.LandmarkType
import com.posepilot.app.domain.model.NudgeDirection
import com.posepilot.app.domain.model.PoseLandmarkPoint
import com.posepilot.app.domain.model.PoseTemplates
import com.posepilot.app.domain.model.Severity
import com.posepilot.app.ui.theme.AlertAmber
import com.posepilot.app.ui.theme.CyberGreen
import com.posepilot.app.ui.theme.CyberTeal
import com.posepilot.app.ui.theme.ErrorNeon
import com.posepilot.app.ui.theme.ObsidianBg
import com.posepilot.app.ui.theme.SlateSurface

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceRequest by viewModel.surfaceRequest.collectAsState()
    val lastCapturedUri by viewModel.lastCapturedUri.collectAsState()
    val poseResults by viewModel.poseResults.collectAsState()
    val postureAnalysis by viewModel.postureAnalysis.collectAsState()
    val coachingIssue by viewModel.coachingIssue.collectAsState()

    val showGrid by viewModel.showGrid.collectAsState()
    val showHorizon by viewModel.showHorizon.collectAsState()
    val deviceRoll by viewModel.deviceRoll.collectAsState()

    val activeTemplate by viewModel.activeTemplate.collectAsState()
    val poseSimilarityScore by viewModel.poseSimilarityScore.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    val countdownValue by viewModel.countdownValue.collectAsState()

    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val overallScore = postureAnalysis?.overallScore ?: 0

    // Trigger haptics when shot readiness crosses 80
    var hasSignaledReady by remember { mutableStateOf(false) }
    LaunchedEffect(overallScore) {
        if (overallScore >= 80) {
            if (!hasSignaledReady) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
                hasSignaledReady = true
            }
        } else {
            hasSignaledReady = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        viewModel.bindCamera(lifecycleOwner)
        onDispose {
            viewModel.unbindCamera()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        surfaceRequest?.let { request ->
            CameraXViewfinder(
                surfaceRequest = request,
                modifier = Modifier.fillMaxSize()
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = CyberGreen)
        }

        // Composition guides canvas (grid + horizon level)
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (showGrid) {
                val gridColor = Color.White.copy(alpha = 0.25f)
                val gridWidth = 1.dp.toPx()

                drawLine(gridColor, Offset(size.width * 0.33f, 0f), Offset(size.width * 0.33f, size.height), gridWidth)
                drawLine(gridColor, Offset(size.width * 0.66f, 0f), Offset(size.width * 0.66f, size.height), gridWidth)

                drawLine(gridColor, Offset(0f, size.height * 0.33f), Offset(size.width, size.height * 0.33f), gridWidth)
                drawLine(gridColor, Offset(0f, size.height * 0.66f), Offset(size.width, size.height * 0.66f), gridWidth)
            }

            if (showHorizon) {
                val isLevel = abs(deviceRoll) < 1.5f
                val levelColor = if (isLevel) CyberGreen else Color.White.copy(alpha = 0.5f)
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                val bracketSize = 20.dp.toPx()
                val gap = 40.dp.toPx()
                val crosshairWidth = 2.dp.toPx()

                drawLine(Color.White.copy(alpha = 0.3f), Offset(centerX - gap - bracketSize, centerY), Offset(centerX - gap, centerY), crosshairWidth)
                drawLine(Color.White.copy(alpha = 0.3f), Offset(centerX + gap, centerY), Offset(centerX + gap + bracketSize, centerY), crosshairWidth)

                val lineLength = 60.dp.toPx()
                val angleRad = Math.toRadians((-deviceRoll).toDouble())
                val cosVal = kotlin.math.cos(angleRad).toFloat()
                val sinVal = kotlin.math.sin(angleRad).toFloat()

                val startOffset = Offset(
                    centerX - lineLength / 2f * cosVal,
                    centerY - lineLength / 2f * sinVal
                )
                val endOffset = Offset(
                    centerX + lineLength / 2f * cosVal,
                    centerY + lineLength / 2f * sinVal
                )

                drawLine(
                    color = levelColor,
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = 3.dp.toPx()
                )

                drawCircle(
                    color = levelColor,
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }
        }

        // Reference pose template silhouette overlay
        activeTemplate?.let { template ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val templateColor = Color.White.copy(alpha = 0.4f)
                val strokeWidth = 2.dp.toPx()
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                fun List<com.posepilot.app.domain.model.TemplatePoint>.findPoint(type: LandmarkType): com.posepilot.app.domain.model.TemplatePoint? {
                    return this.find { it.type == type }
                }

                fun drawTemplateConnection(start: LandmarkType, end: LandmarkType) {
                    val p1 = template.points.findPoint(start)
                    val p2 = template.points.findPoint(end)
                    if (p1 != null && p2 != null) {
                        drawLine(
                            color = templateColor,
                            start = Offset(p1.x * size.width, p1.y * size.height),
                            end = Offset(p2.x * size.width, p2.y * size.height),
                            strokeWidth = strokeWidth,
                            pathEffect = pathEffect
                        )
                    }
                }

                // Bones
                drawTemplateConnection(LandmarkType.LEFT_SHOULDER, LandmarkType.RIGHT_SHOULDER)
                drawTemplateConnection(LandmarkType.LEFT_SHOULDER, LandmarkType.LEFT_HIP)
                drawTemplateConnection(LandmarkType.RIGHT_SHOULDER, LandmarkType.RIGHT_HIP)
                drawTemplateConnection(LandmarkType.LEFT_HIP, LandmarkType.RIGHT_HIP)

                drawTemplateConnection(LandmarkType.LEFT_SHOULDER, LandmarkType.LEFT_ELBOW)
                drawTemplateConnection(LandmarkType.LEFT_ELBOW, LandmarkType.LEFT_WRIST)
                drawTemplateConnection(LandmarkType.RIGHT_SHOULDER, LandmarkType.RIGHT_ELBOW)
                drawTemplateConnection(LandmarkType.RIGHT_ELBOW, LandmarkType.RIGHT_WRIST)

                drawTemplateConnection(LandmarkType.LEFT_HIP, LandmarkType.LEFT_KNEE)
                drawTemplateConnection(LandmarkType.LEFT_KNEE, LandmarkType.LEFT_ANKLE)
                drawTemplateConnection(LandmarkType.RIGHT_HIP, LandmarkType.RIGHT_KNEE)
                drawTemplateConnection(LandmarkType.RIGHT_KNEE, LandmarkType.RIGHT_ANKLE)

                drawTemplateConnection(LandmarkType.NOSE, LandmarkType.LEFT_EAR)
                drawTemplateConnection(LandmarkType.NOSE, LandmarkType.RIGHT_EAR)

                // Joints
                template.points.forEach { pt ->
                    drawCircle(
                        color = templateColor,
                        radius = 4.dp.toPx(),
                        center = Offset(pt.x * size.width, pt.y * size.height)
                    )
                }
            }
        }

        // Top guide controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (showGrid) CyberGreen.copy(alpha = 0.2f) else SlateSurface.copy(alpha = 0.8f))
                    .border(1.dp, if (showGrid) CyberGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .clickable { viewModel.toggleGrid() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GRID",
                    color = if (showGrid) CyberGreen else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (showHorizon) CyberTeal.copy(alpha = 0.2f) else SlateSurface.copy(alpha = 0.8f))
                    .border(1.dp, if (showHorizon) CyberTeal.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .clickable { viewModel.toggleHorizon() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LEVEL",
                    color = if (showHorizon) CyberTeal else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
                    .background(SlateSurface.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Top center pose similarity match indicator
        poseSimilarityScore?.let { score ->
            val isMatch = score >= 75
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isMatch) CyberGreen.copy(alpha = 0.2f) else SlateSurface.copy(alpha = 0.8f))
                        .border(1.dp, if (isMatch) CyberGreen else Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Match: $score%",
                        color = if (isMatch) CyberGreen else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live skeletal tracking overlay canvas with color coding
        poseResults?.let { landmarksList ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()

                fun List<PoseLandmarkPoint>.findPoint(type: LandmarkType): PoseLandmarkPoint? {
                    return this.find { it.type == type }
                }

                // Map active coaching alert types to specific skeleton colors
                val activeIssueType = coachingIssue?.type
                val activeIssueSeverity = coachingIssue?.severity

                fun getLineColorFor(type1: LandmarkType, type2: LandmarkType): Color {
                    if (activeIssueType == null || activeIssueSeverity == null) return CyberGreen
                    val alertColor = if (activeIssueSeverity == Severity.NEEDS_CORRECTION) ErrorNeon else AlertAmber
                    
                    when (activeIssueType) {
                        IssueType.SHOULDER_TILT -> {
                            if ((type1 == LandmarkType.LEFT_SHOULDER && type2 == LandmarkType.RIGHT_SHOULDER) ||
                                (type1 == LandmarkType.LEFT_SHOULDER && type2 == LandmarkType.LEFT_HIP) ||
                                (type1 == LandmarkType.RIGHT_SHOULDER && type2 == LandmarkType.RIGHT_HIP)) {
                                return alertColor
                            }
                        }
                        IssueType.HEAD_TILT -> {
                            if (type1 == LandmarkType.NOSE || type2 == LandmarkType.NOSE ||
                                type1 == LandmarkType.LEFT_EAR || type2 == LandmarkType.LEFT_EAR ||
                                type1 == LandmarkType.RIGHT_EAR || type2 == LandmarkType.RIGHT_EAR) {
                                return alertColor
                            }
                        }
                        IssueType.SPINE_LEAN -> {
                            if (type1 == LandmarkType.LEFT_HIP || type2 == LandmarkType.LEFT_HIP ||
                                type1 == LandmarkType.RIGHT_HIP || type2 == LandmarkType.RIGHT_HIP) {
                                return alertColor
                            }
                        }
                        IssueType.SUBJECT_SCALE -> {
                            return alertColor
                        }
                        else -> {}
                    }
                    return CyberGreen
                }

                fun drawConnection(start: LandmarkType, end: LandmarkType) {
                    val p1 = landmarksList.findPoint(start)
                    val p2 = landmarksList.findPoint(end)
                    if (p1 != null && p2 != null && p1.visibility > 0.5f && p2.visibility > 0.5f) {
                        drawLine(
                            color = getLineColorFor(start, end),
                            start = Offset(p1.x * size.width, p1.y * size.height),
                            end = Offset(p2.x * size.width, p2.y * size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                }

                // Torso connections
                drawConnection(LandmarkType.LEFT_SHOULDER, LandmarkType.RIGHT_SHOULDER)
                drawConnection(LandmarkType.LEFT_SHOULDER, LandmarkType.LEFT_HIP)
                drawConnection(LandmarkType.RIGHT_SHOULDER, LandmarkType.RIGHT_HIP)
                drawConnection(LandmarkType.LEFT_HIP, LandmarkType.RIGHT_HIP)

                // Arm connections
                drawConnection(LandmarkType.LEFT_SHOULDER, LandmarkType.LEFT_ELBOW)
                drawConnection(LandmarkType.LEFT_ELBOW, LandmarkType.LEFT_WRIST)
                drawConnection(LandmarkType.RIGHT_SHOULDER, LandmarkType.RIGHT_ELBOW)
                drawConnection(LandmarkType.RIGHT_ELBOW, LandmarkType.RIGHT_WRIST)

                // Leg connections
                drawConnection(LandmarkType.LEFT_HIP, LandmarkType.LEFT_KNEE)
                drawConnection(LandmarkType.LEFT_KNEE, LandmarkType.LEFT_ANKLE)
                drawConnection(LandmarkType.RIGHT_HIP, LandmarkType.RIGHT_KNEE)
                drawConnection(LandmarkType.RIGHT_KNEE, LandmarkType.RIGHT_ANKLE)

                // Head/Face connections
                drawConnection(LandmarkType.NOSE, LandmarkType.LEFT_EAR)
                drawConnection(LandmarkType.NOSE, LandmarkType.RIGHT_EAR)

                // Draw joints
                landmarksList.forEach { landmark ->
                    if (landmark.visibility > 0.5f) {
                        val jointColor = if (activeIssueType != null && activeIssueSeverity != null) {
                            if (activeIssueSeverity == Severity.NEEDS_CORRECTION) ErrorNeon else AlertAmber
                        } else {
                            CyberTeal
                        }
                        drawCircle(
                            color = jointColor,
                            radius = 6.dp.toPx(),
                            center = Offset(landmark.x * size.width, landmark.y * size.height)
                        )
                    }
                }
            }
        }

        // Flashing nudge arrows at screen edges
        val nudgeDirection = coachingIssue?.nudgeDirection
        if (nudgeDirection != null) {
            val infiniteTransition = rememberInfiniteTransition(label = "nudge")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = when (nudgeDirection) {
                    NudgeDirection.LEFT -> Alignment.CenterStart
                    NudgeDirection.RIGHT -> Alignment.CenterEnd
                    NudgeDirection.UP -> Alignment.TopCenter
                    NudgeDirection.DOWN -> Alignment.BottomCenter
                    else -> Alignment.Center
                }
            ) {
                if (nudgeDirection == NudgeDirection.LEFT ||
                    nudgeDirection == NudgeDirection.RIGHT ||
                    nudgeDirection == NudgeDirection.UP ||
                    nudgeDirection == NudgeDirection.DOWN
                ) {
                    val rotation = when (nudgeDirection) {
                        NudgeDirection.LEFT -> 0f
                        NudgeDirection.RIGHT -> 180f
                        NudgeDirection.UP -> 90f
                        NudgeDirection.DOWN -> 270f
                        else -> 0f
                    }
                    val arrowColor = if (coachingIssue?.severity == Severity.NEEDS_CORRECTION) ErrorNeon else AlertAmber
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Move indicator",
                        tint = arrowColor.copy(alpha = alpha),
                        modifier = Modifier
                            .padding(32.dp)
                            .size(64.dp)
                            .graphicsLayer(rotationZ = rotation)
                    )
                }
            }
        }

        // Floating glassmorphic coaching bubble
        coachingIssue?.let { issue ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 188.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (issue.severity == Severity.NEEDS_CORRECTION) ErrorNeon.copy(alpha = 0.5f) else CyberGreen.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Coaching Nudge",
                            tint = if (issue.severity == Severity.NEEDS_CORRECTION) ErrorNeon else CyberGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = issue.message,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Horizontal Template Carousel selector
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 124.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "None" option
                item {
                    val isSelected = activeTemplate == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) CyberGreen.copy(alpha = 0.2f) else SlateSurface.copy(alpha = 0.7f))
                            .border(1.dp, if (isSelected) CyberGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .clickable { viewModel.selectTemplate(null) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "None",
                            color = if (isSelected) CyberGreen else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(PoseTemplates.all) { template ->
                    val isSelected = activeTemplate?.id == template.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) CyberGreen.copy(alpha = 0.2f) else SlateSurface.copy(alpha = 0.7f))
                            .border(1.dp, if (isSelected) CyberGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .clickable { viewModel.selectTemplate(template) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = template.name,
                            color = if (isSelected) CyberGreen else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SlateSurface.copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable(enabled = true, onClick = onNavigateToGallery),
                    contentAlignment = Alignment.Center
                ) {
                    if (lastCapturedUri != null) {
                        AsyncImage(
                            model = lastCapturedUri,
                            contentDescription = "Gallery Thumbnail",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            viewModel.takePhoto(
                                onPhotoSaved = { /* Optional visual flash cue */ },
                                onError = { it.printStackTrace() }
                            )
                        }
                )

                IconButton(
                    onClick = { viewModel.toggleCamera(lifecycleOwner) },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SlateSurface.copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showSettings = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Coaching Settings",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showSettings = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val audioEnabled by viewModel.audioEnabled.collectAsState()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Voice coaching hints",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Switch(
                                checked = audioEnabled,
                                onCheckedChange = { viewModel.toggleAudio() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberGreen,
                                    checkedTrackColor = CyberGreen.copy(alpha = 0.4f),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val sensitivity by viewModel.sensitivity.collectAsState()
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Coaching strictness",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${String.format("%.1f", sensitivity)}x",
                                    color = CyberGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = sensitivity,
                                onValueChange = { viewModel.updateSensitivity(it) },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyberGreen,
                                    activeTrackColor = CyberGreen,
                                    inactiveTrackColor = Color.DarkGray
                                )
                            )
                            Text(
                                text = "Higher values make heuristics stricter",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Smart Shutter Switch Row
                        val smartShutterEnabled by viewModel.smartShutterEnabled.collectAsState()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Auto-Shutter (Countdown)",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Switch(
                                checked = smartShutterEnabled,
                                onCheckedChange = { viewModel.toggleSmartShutter() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberGreen,
                                    checkedTrackColor = CyberGreen.copy(alpha = 0.4f),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }

                        if (smartShutterEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val smartShutterDuration by viewModel.smartShutterDuration.collectAsState()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Countdown duration",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(3, 5).forEach { secs ->
                                        val isSelected = smartShutterDuration == secs
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) CyberGreen.copy(alpha = 0.2f) else SlateSurface.copy(alpha = 0.8f))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) CyberGreen else Color.White.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { viewModel.updateSmartShutterDuration(secs) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${secs}s",
                                                color = if (isSelected) CyberGreen else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pulse timer countdown visual overlay
        countdownValue?.let { count ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "countdown")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "scale"
                )
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 0.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "alpha"
                )

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .border(4.dp, CyberGreen.copy(alpha = alpha), CircleShape)
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(SlateSurface.copy(alpha = 0.9f))
                        .border(2.dp, CyberGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        color = CyberGreen,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
