package com.posepilot.app.domain.analysis

import com.posepilot.app.domain.model.PoseLandmarkPoint
import com.posepilot.app.domain.model.PoseTemplate
import kotlin.math.max
import kotlin.math.sqrt

object PoseSimilarityCalculator {

    fun calculate(
        livePoints: List<PoseLandmarkPoint>,
        template: PoseTemplate
    ): Int {
        // Filter points that are visible in the live landmarks stream
        val visibleLivePoints = livePoints.filter { it.visibility > 0.5f }
        if (visibleLivePoints.size < 4) return 0

        // Find intersecting landmarks between live points and template points
        val commonLandmarkTypes = visibleLivePoints.map { it.type }
            .intersect(template.points.map { it.type }.toSet())

        if (commonLandmarkTypes.size < 4) return 0

        // 1. Normalize live points
        val matchingLive = visibleLivePoints.filter { it.type in commonLandmarkTypes }
        val liveMinX = matchingLive.minOf { it.x }
        val liveMaxX = matchingLive.maxOf { it.x }
        val liveMinY = matchingLive.minOf { it.y }
        val liveMaxY = matchingLive.maxOf { it.y }

        val liveHeight = liveMaxY - liveMinY
        val liveCenterX = (liveMinX + liveMaxX) / 2f
        val liveCenterY = (liveMinY + liveMaxY) / 2f

        val refHeight = 0.6f
        val liveScale = if (liveHeight > 0.001f) refHeight / liveHeight else 1f

        val normalizedLive = matchingLive.associate { point ->
            val nx = (point.x - liveCenterX) * liveScale + 0.5f
            val ny = (point.y - liveCenterY) * liveScale + 0.5f
            point.type to Pair(nx, ny)
        }

        // 2. Normalize template points
        val matchingTemplate = template.points.filter { it.type in commonLandmarkTypes }
        val tempMinX = matchingTemplate.minOf { it.x }
        val tempMaxX = matchingTemplate.maxOf { it.x }
        val tempMinY = matchingTemplate.minOf { it.y }
        val tempMaxY = matchingTemplate.maxOf { it.y }

        val tempHeight = tempMaxY - tempMinY
        val tempCenterX = (tempMinX + tempMaxX) / 2f
        val tempCenterY = (tempMinY + tempMaxY) / 2f
        val tempScale = if (tempHeight > 0.001f) refHeight / tempHeight else 1f

        val normalizedTemplate = matchingTemplate.associate { point ->
            val nx = (point.x - tempCenterX) * tempScale + 0.5f
            val ny = (point.y - tempCenterY) * tempScale + 0.5f
            point.type to Pair(nx, ny)
        }

        // 3. Compute Euclidean Distance Error
        var totalDistance = 0f
        var count = 0
        for (type in commonLandmarkTypes) {
            val live = normalizedLive[type]
            val temp = normalizedTemplate[type]
            if (live != null && temp != null) {
                val dx = live.first - temp.first
                val dy = live.second - temp.second
                totalDistance += sqrt(dx * dx + dy * dy)
                count++
            }
        }

        if (count == 0) return 0
        val avgDistance = totalDistance / count

        // Scale error to 0..100
        // An average error of 0.18 normalized units is considered a 0% match.
        val maxAcceptableDistance = 0.18f
        val similarityScore = max(0f, 100f - (avgDistance / maxAcceptableDistance) * 100f)

        return similarityScore.toInt()
    }
}
