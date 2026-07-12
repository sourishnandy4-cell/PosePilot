package com.posepilot.app.domain.analysis

import com.posepilot.app.domain.model.PoseLandmarkPoint
import com.posepilot.app.domain.model.PoseTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseSimilarityCalculatorTest {

    @Test
    fun testSelfSimilarityIsPerfectScore() {
        val template = PoseTemplates.Standing
        val livePoints = template.points.map { pt ->
            PoseLandmarkPoint(pt.type, pt.x, pt.y, 0f, 0.9f)
        }

        val score = PoseSimilarityCalculator.calculate(livePoints, template)
        assertEquals(100, score)
    }

    @Test
    fun testTranslatedPoseSimilarityIsHigh() {
        val template = PoseTemplates.Standing
        // Shift every point in the template by dx = +0.1, dy = -0.15
        val dx = 0.1f
        val dy = -0.15f
        val livePoints = template.points.map { pt ->
            PoseLandmarkPoint(pt.type, pt.x + dx, pt.y + dy, 0f, 0.9f)
        }

        val score = PoseSimilarityCalculator.calculate(livePoints, template)
        // Score should be extremely close to 100 since the calculator centers the pose
        assertTrue("Score should be > 95, got: $score", score >= 95)
    }

    @Test
    fun testScaledPoseSimilarityIsHigh() {
        val template = PoseTemplates.Standing
        // Scale template points by 0.7 around the center (0.5, 0.5)
        val factor = 0.7f
        val livePoints = template.points.map { pt ->
            val sx = (pt.x - 0.5f) * factor + 0.5f
            val sy = (pt.y - 0.5f) * factor + 0.5f
            PoseLandmarkPoint(pt.type, sx, sy, 0f, 0.9f)
        }

        val score = PoseSimilarityCalculator.calculate(livePoints, template)
        // Score should be high since the calculator normalizes the bounding box height
        assertTrue("Score should be > 95, got: $score", score >= 95)
    }

    @Test
    fun testMismatchPoseSimilarityIsLow() {
        val template = PoseTemplates.Standing
        // Create an entirely flat pose where all joints collapsed at (0.5, 0.5)
        val livePoints = template.points.map { pt ->
            PoseLandmarkPoint(pt.type, 0.5f, 0.5f, 0f, 0.9f)
        }

        val score = PoseSimilarityCalculator.calculate(livePoints, template)
        // Collapsed points should yield 0 or low score
        assertTrue("Score should be low, got: $score", score < 30)
    }
}
