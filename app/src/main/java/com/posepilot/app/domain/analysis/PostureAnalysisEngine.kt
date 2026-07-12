package com.posepilot.app.domain.analysis

import com.posepilot.app.domain.model.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

class PostureAnalysisEngine(
    private val sensitivityMultiplier: Float = 1.0f
) {

    fun analyze(
        landmarks: List<PoseLandmarkPoint>,
        timestampMs: Long = System.currentTimeMillis()
    ): PostureAnalysisResult {
        // If there are no landmarks, or critical landmarks are missing/invisible, return empty result
        val visibleLandmarks = landmarks.filter { it.visibility > 0.5f }
        if (visibleLandmarks.size < 6) {
            return PostureAnalysisResult(
                overallScore = 0,
                issues = listOf(
                    PostureIssue(
                        type = IssueType.FRAME_POSITION,
                        severity = Severity.NEEDS_CORRECTION,
                        message = "Frame a subject to begin coaching"
                    )
                ),
                timestampMs = timestampMs
            )
        }

        // Find critical points
        val leftShoulder = visibleLandmarks.findPoint(LandmarkType.LEFT_SHOULDER)
        val rightShoulder = visibleLandmarks.findPoint(LandmarkType.RIGHT_SHOULDER)
        val leftEar = visibleLandmarks.findPoint(LandmarkType.LEFT_EAR)
        val rightEar = visibleLandmarks.findPoint(LandmarkType.RIGHT_EAR)
        val leftHip = visibleLandmarks.findPoint(LandmarkType.LEFT_HIP)
        val rightHip = visibleLandmarks.findPoint(LandmarkType.RIGHT_HIP)

        val issues = mutableListOf<PostureIssue>()

        // 1. Shoulder Level Heuristic
        var shoulderScore = 100f
        if (leftShoulder != null && rightShoulder != null) {
            val angle = angleFromHorizontalDegrees(leftShoulder, rightShoulder)
            val absAngle = abs(angle)
            val threshold = 6f / sensitivityMultiplier
            if (absAngle > threshold) {
                issues.add(
                    PostureIssue(
                        type = IssueType.SHOULDER_TILT,
                        severity = Severity.NEEDS_CORRECTION,
                        message = "Level your shoulders a little",
                        nudgeDirection = if (angle > 0) NudgeDirection.RIGHT else NudgeDirection.LEFT
                    )
                )
            }
            shoulderScore = (100f - (absAngle / 15f) * 100f).coerceIn(0f, 100f)
        }

        // 2. Head Tilt Heuristic
        var headScore = 100f
        if (leftEar != null && rightEar != null) {
            val angle = angleFromHorizontalDegrees(leftEar, rightEar)
            val absAngle = abs(angle)
            val threshold = 8f / sensitivityMultiplier
            if (absAngle > threshold) {
                issues.add(
                    PostureIssue(
                        type = IssueType.HEAD_TILT,
                        severity = Severity.MINOR,
                        message = "Keep your head straight",
                        nudgeDirection = if (angle > 0) NudgeDirection.RIGHT else NudgeDirection.LEFT
                    )
                )
            }
            headScore = (100f - (absAngle / 20f) * 100f).coerceIn(0f, 100f)
        }

        // Calculate bounding box center and height of visible landmarks
        val minX = visibleLandmarks.minOf { it.x }
        val maxX = visibleLandmarks.maxOf { it.x }
        val minY = visibleLandmarks.minOf { it.y }
        val maxY = visibleLandmarks.maxOf { it.y }

        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val boundingBoxHeight = maxY - minY

        // 3. Subject Scale Heuristic
        var scaleScore = 100f
        val minTargetScale = 0.55f * sensitivityMultiplier
        val maxTargetScale = 0.85f / sensitivityMultiplier
        if (boundingBoxHeight < minTargetScale) {
            issues.add(
                PostureIssue(
                    type = IssueType.SUBJECT_SCALE,
                    severity = Severity.NEEDS_CORRECTION,
                    message = "Step a little closer",
                    nudgeDirection = NudgeDirection.CLOSER
                )
            )
            scaleScore = (boundingBoxHeight / minTargetScale * 100f).coerceIn(0f, 100f)
        } else if (boundingBoxHeight > maxTargetScale) {
            issues.add(
                PostureIssue(
                    type = IssueType.SUBJECT_SCALE,
                    severity = Severity.NEEDS_CORRECTION,
                    message = "Step back for more room",
                    nudgeDirection = NudgeDirection.FARTHER
                )
            )
            scaleScore = ((1f - boundingBoxHeight) / (1f - maxTargetScale) * 100f).coerceIn(0f, 100f)
        }

        // 4. Bounding Box Position (Rule of Thirds intersections)
        var frameScore = 100f
        val powerPoints = listOf(
            Pair(0.33f, 0.33f),
            Pair(0.66f, 0.33f),
            Pair(0.33f, 0.66f),
            Pair(0.66f, 0.66f)
        )
        // Find nearest rule of thirds power point
        var minDistance = Float.MAX_VALUE
        var closestPt = powerPoints[0]
        for (pt in powerPoints) {
            val dist = sqrt((centerX - pt.first) * (centerX - pt.first) + (centerY - pt.second) * (centerY - pt.second))
            if (dist < minDistance) {
                minDistance = dist
                closestPt = pt
            }
        }

        val dx = centerX - closestPt.first
        val dy = centerY - closestPt.second
        val thresholdOffset = 0.15f / sensitivityMultiplier
        if (abs(dx) > thresholdOffset || abs(dy) > thresholdOffset) {
            val horizontalNudge = when {
                dx > thresholdOffset -> NudgeDirection.LEFT
                dx < -thresholdOffset -> NudgeDirection.RIGHT
                else -> null
            }
            val verticalNudge = when {
                dy > thresholdOffset -> NudgeDirection.UP
                dy < -thresholdOffset -> NudgeDirection.DOWN
                else -> null
            }
            // Prioritize horizontal alignment message, else vertical
            val message = when {
                horizontalNudge == NudgeDirection.LEFT -> "Move slightly to your left"
                horizontalNudge == NudgeDirection.RIGHT -> "Move slightly to your right"
                verticalNudge == NudgeDirection.UP -> "Move slightly up in the frame"
                verticalNudge == NudgeDirection.DOWN -> "Move slightly down in the frame"
                else -> "Center yourself inside the frame grid"
            }
            issues.add(
                PostureIssue(
                    type = IssueType.FRAME_POSITION,
                    severity = Severity.MINOR,
                    message = message,
                    nudgeDirection = horizontalNudge ?: verticalNudge
                )
            )
            frameScore = (100f - (minDistance / 0.3f) * 100f).coerceIn(0f, 100f)
        }

        // 5. Spine Lean Heuristic
        var spineScore = 100f
        if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null) {
            val shoulderMidX = (leftShoulder.x + rightShoulder.x) / 2f
            val shoulderMidY = (leftShoulder.y + rightShoulder.y) / 2f
            val hipMidX = (leftHip.x + rightHip.x) / 2f
            val hipMidY = (leftHip.y + rightHip.y) / 2f

            // dx/dy relative to vertical line (hip to shoulder)
            val dy = hipMidY - shoulderMidY
            val dx = hipMidX - shoulderMidX
            if (dy > 0f) {
                val leanAngleRad = atan2(dx, dy)
                val leanAngleDeg = Math.toDegrees(leanAngleRad.toDouble()).toFloat()
                val absLean = abs(leanAngleDeg)
                val threshold = 15f / sensitivityMultiplier
                if (absLean > threshold) {
                    issues.add(
                        PostureIssue(
                            type = IssueType.SPINE_LEAN,
                            severity = Severity.INFO,
                            message = "Straighten your spine slightly",
                            nudgeDirection = if (leanAngleDeg > 0) NudgeDirection.LEFT else NudgeDirection.RIGHT
                        )
                    )
                }
                spineScore = (100f - (absLean / 30f) * 100f).coerceIn(0f, 100f)
            }
        }

        // Sort issues by severity (NEEDS_CORRECTION > MINOR > INFO)
        val sortedIssues = issues.sortedWith { i1, i2 ->
            i2.severity.ordinal.compareTo(i1.severity.ordinal)
        }

        // Calculate weighted score
        val overallScore = (
            frameScore * 0.30f +
            scaleScore * 0.25f +
            shoulderScore * 0.20f +
            headScore * 0.15f +
            spineScore * 0.10f
        ).toInt().coerceIn(0, 100)

        return PostureAnalysisResult(
            overallScore = overallScore,
            issues = sortedIssues,
            timestampMs = timestampMs
        )
    }

    private fun List<PoseLandmarkPoint>.findPoint(type: LandmarkType): PoseLandmarkPoint? {
        return this.find { it.type == type }
    }

    private fun angleFromHorizontalDegrees(a: PoseLandmarkPoint, b: PoseLandmarkPoint): Float {
        val dy = b.y - a.y
        val dx = b.x - a.x
        return Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    }
}
