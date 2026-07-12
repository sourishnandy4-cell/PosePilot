package com.posepilot.app.domain.analysis

import com.posepilot.app.domain.model.IssueType
import com.posepilot.app.domain.model.LandmarkType
import com.posepilot.app.domain.model.NudgeDirection
import com.posepilot.app.domain.model.PoseLandmarkPoint
import com.posepilot.app.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostureAnalysisEngineTest {

    private val engine = PostureAnalysisEngine()

    // Helper to generate a baseline aligned pose centered around a rule of thirds intersection (e.g. 0.33, 0.33)
    private fun createAlignedPose(
        centerX: Float = 0.33f,
        centerY: Float = 0.33f,
        scale: Float = 0.6f,
        shoulderAngleDeg: Float = 0f,
        headAngleDeg: Float = 0f
    ): List<PoseLandmarkPoint> {
        val halfHeight = scale / 1.4f
        val halfWidth = halfHeight * 0.5f

        val dxShoulder = halfWidth * 2f
        val dyShoulder = dxShoulder * kotlin.math.tan(Math.toRadians(shoulderAngleDeg.toDouble())).toFloat()
        val shoulderYLeft = centerY - halfHeight / 4f - dyShoulder / 2f
        val shoulderYRight = centerY - halfHeight / 4f + dyShoulder / 2f

        val dxEar = 0.1f
        val dyEar = dxEar * kotlin.math.tan(Math.toRadians(headAngleDeg.toDouble())).toFloat()
        val earYLeft = centerY - halfHeight * 0.8f - dyEar / 2f
        val earYRight = centerY - halfHeight * 0.8f + dyEar / 2f

        return listOf(
            PoseLandmarkPoint(LandmarkType.NOSE, centerX, centerY - halfHeight * 0.9f, 0f, 0.9f),
            PoseLandmarkPoint(LandmarkType.LEFT_EAR, centerX - 0.05f, earYLeft, 0f, 0.9f),
            PoseLandmarkPoint(LandmarkType.RIGHT_EAR, centerX + 0.05f, earYRight, 0f, 0.9f),
            PoseLandmarkPoint(LandmarkType.LEFT_SHOULDER, centerX - halfWidth, shoulderYLeft, 0f, 0.9f),
            PoseLandmarkPoint(LandmarkType.RIGHT_SHOULDER, centerX + halfWidth, shoulderYRight, 0f, 0.9f),
            PoseLandmarkPoint(LandmarkType.LEFT_HIP, centerX - halfWidth, centerY + halfHeight / 2f, 0f, 0.9f),
            PoseLandmarkPoint(LandmarkType.RIGHT_HIP, centerX + halfWidth, centerY + halfHeight / 2f, 0f, 0.9f)
        )
    }

    @Test
    fun testEmptyLandmarksRequiresSubject() {
        val result = engine.analyze(emptyList())
        assertEquals(0, result.overallScore)
        assertEquals(1, result.issues.size)
        assertEquals(IssueType.FRAME_POSITION, result.issues[0].type)
        assertEquals(Severity.NEEDS_CORRECTION, result.issues[0].severity)
        assertEquals("Frame a subject to begin coaching", result.issues[0].message)
    }

    @Test
    fun testPerfectAlignmentHighScoreNoIssues() {
        val landmarks = createAlignedPose()
        val result = engine.analyze(landmarks)
        // High score expected for aligned, properly scaled & positioned landmarks
        assertTrue("Score should be high, got: ${result.overallScore}", result.overallScore > 90)
        assertEquals(0, result.issues.size)
    }

    @Test
    fun testShoulderTiltTriggersNeedsCorrection() {
        // Create pose with a significant shoulder tilt
        val landmarks = createAlignedPose(shoulderAngleDeg = 12f)
        val result = engine.analyze(landmarks)
        
        assertTrue("Should detect shoulder tilt issue", result.issues.any { it.type == IssueType.SHOULDER_TILT })
        val shoulderIssue = result.issues.first { it.type == IssueType.SHOULDER_TILT }
        assertEquals(Severity.NEEDS_CORRECTION, shoulderIssue.severity)
        assertEquals("Level your shoulders a little", shoulderIssue.message)
    }

    @Test
    fun testHeadTiltTriggersMinorIssue() {
        // Create pose with significant head tilt
        val landmarks = createAlignedPose(headAngleDeg = 15f)
        val result = engine.analyze(landmarks)
        
        assertTrue("Should detect head tilt issue", result.issues.any { it.type == IssueType.HEAD_TILT })
        val headIssue = result.issues.first { it.type == IssueType.HEAD_TILT }
        assertEquals(Severity.MINOR, headIssue.severity)
        assertEquals("Keep your head straight", headIssue.message)
    }

    @Test
    fun testScaleTooSmallTriggersCloserNudge() {
        // Create pose with very small scale (too far away)
        val landmarks = createAlignedPose(scale = 0.2f)
        val result = engine.analyze(landmarks)
        
        assertTrue("Should detect subject scale issue", result.issues.any { it.type == IssueType.SUBJECT_SCALE })
        val scaleIssue = result.issues.first { it.type == IssueType.SUBJECT_SCALE }
        assertEquals(Severity.NEEDS_CORRECTION, scaleIssue.severity)
        assertEquals(NudgeDirection.CLOSER, scaleIssue.nudgeDirection)
        assertEquals("Step a little closer", scaleIssue.message)
    }

    @Test
    fun testScaleTooLargeTriggersFartherNudge() {
        // Create pose with very large scale (too close)
        val landmarks = createAlignedPose(scale = 0.95f)
        val result = engine.analyze(landmarks)
        
        assertTrue("Should detect subject scale issue", result.issues.any { it.type == IssueType.SUBJECT_SCALE })
        val scaleIssue = result.issues.first { it.type == IssueType.SUBJECT_SCALE }
        assertEquals(Severity.NEEDS_CORRECTION, scaleIssue.severity)
        assertEquals(NudgeDirection.FARTHER, scaleIssue.nudgeDirection)
        assertEquals("Step back for more room", scaleIssue.message)
    }

    @Test
    fun testOffCenterFramingTriggersFramePosition() {
        // Create pose away from all four Rule of Thirds power points (e.g. exactly center (0.5, 0.5))
        val landmarks = createAlignedPose(centerX = 0.5f, centerY = 0.5f)
        val result = engine.analyze(landmarks)
        
        assertTrue("Should detect off center framing", result.issues.any { it.type == IssueType.FRAME_POSITION })
        val frameIssue = result.issues.first { it.type == IssueType.FRAME_POSITION }
        assertEquals(Severity.MINOR, frameIssue.severity)
    }
}
