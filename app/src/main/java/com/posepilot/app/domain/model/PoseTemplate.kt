package com.posepilot.app.domain.model

data class PoseTemplate(
    val id: String,
    val name: String,
    val description: String,
    val points: List<TemplatePoint>
)

data class TemplatePoint(
    val type: LandmarkType,
    val x: Float, // normalized coordinate
    val y: Float
)

object PoseTemplates {
    val Standing = PoseTemplate(
        id = "standing",
        name = "Standing",
        description = "Standard front-facing standing posture",
        points = listOf(
            TemplatePoint(LandmarkType.NOSE, 0.5f, 0.20f),
            TemplatePoint(LandmarkType.LEFT_EAR, 0.46f, 0.17f),
            TemplatePoint(LandmarkType.RIGHT_EAR, 0.54f, 0.17f),
            TemplatePoint(LandmarkType.LEFT_SHOULDER, 0.40f, 0.28f),
            TemplatePoint(LandmarkType.RIGHT_SHOULDER, 0.60f, 0.28f),
            TemplatePoint(LandmarkType.LEFT_ELBOW, 0.37f, 0.45f),
            TemplatePoint(LandmarkType.RIGHT_ELBOW, 0.63f, 0.45f),
            TemplatePoint(LandmarkType.LEFT_WRIST, 0.37f, 0.62f),
            TemplatePoint(LandmarkType.RIGHT_WRIST, 0.63f, 0.62f),
            TemplatePoint(LandmarkType.LEFT_HIP, 0.42f, 0.58f),
            TemplatePoint(LandmarkType.RIGHT_HIP, 0.58f, 0.58f),
            TemplatePoint(LandmarkType.LEFT_KNEE, 0.42f, 0.76f),
            TemplatePoint(LandmarkType.RIGHT_KNEE, 0.58f, 0.76f),
            TemplatePoint(LandmarkType.LEFT_ANKLE, 0.42f, 0.94f),
            TemplatePoint(LandmarkType.RIGHT_ANKLE, 0.58f, 0.94f)
        )
    )

    val Seated = PoseTemplate(
        id = "seated",
        name = "Seated",
        description = "Relaxed seated portrait style",
        points = listOf(
            TemplatePoint(LandmarkType.NOSE, 0.50f, 0.25f),
            TemplatePoint(LandmarkType.LEFT_EAR, 0.46f, 0.22f),
            TemplatePoint(LandmarkType.RIGHT_EAR, 0.54f, 0.22f),
            TemplatePoint(LandmarkType.LEFT_SHOULDER, 0.40f, 0.35f),
            TemplatePoint(LandmarkType.RIGHT_SHOULDER, 0.60f, 0.35f),
            TemplatePoint(LandmarkType.LEFT_ELBOW, 0.34f, 0.53f),
            TemplatePoint(LandmarkType.RIGHT_ELBOW, 0.66f, 0.53f),
            TemplatePoint(LandmarkType.LEFT_WRIST, 0.42f, 0.65f),
            TemplatePoint(LandmarkType.RIGHT_WRIST, 0.58f, 0.65f),
            TemplatePoint(LandmarkType.LEFT_HIP, 0.42f, 0.68f),
            TemplatePoint(LandmarkType.RIGHT_HIP, 0.58f, 0.68f),
            TemplatePoint(LandmarkType.LEFT_KNEE, 0.33f, 0.82f),
            TemplatePoint(LandmarkType.RIGHT_KNEE, 0.67f, 0.82f),
            TemplatePoint(LandmarkType.LEFT_ANKLE, 0.38f, 0.95f),
            TemplatePoint(LandmarkType.RIGHT_ANKLE, 0.62f, 0.95f)
        )
    )

    val Candid = PoseTemplate(
        id = "candid",
        name = "Candid",
        description = "Casual leaning pose with hand on hip",
        points = listOf(
            TemplatePoint(LandmarkType.NOSE, 0.52f, 0.21f),
            TemplatePoint(LandmarkType.LEFT_EAR, 0.48f, 0.18f),
            TemplatePoint(LandmarkType.RIGHT_EAR, 0.56f, 0.18f),
            TemplatePoint(LandmarkType.LEFT_SHOULDER, 0.42f, 0.31f),
            TemplatePoint(LandmarkType.RIGHT_SHOULDER, 0.61f, 0.30f),
            TemplatePoint(LandmarkType.LEFT_ELBOW, 0.32f, 0.48f),
            TemplatePoint(LandmarkType.RIGHT_ELBOW, 0.68f, 0.44f),
            TemplatePoint(LandmarkType.LEFT_WRIST, 0.42f, 0.58f),
            TemplatePoint(LandmarkType.RIGHT_WRIST, 0.68f, 0.62f),
            TemplatePoint(LandmarkType.LEFT_HIP, 0.45f, 0.62f),
            TemplatePoint(LandmarkType.RIGHT_HIP, 0.59f, 0.61f),
            TemplatePoint(LandmarkType.LEFT_KNEE, 0.44f, 0.79f),
            TemplatePoint(LandmarkType.RIGHT_KNEE, 0.61f, 0.79f),
            TemplatePoint(LandmarkType.LEFT_ANKLE, 0.47f, 0.95f),
            TemplatePoint(LandmarkType.RIGHT_ANKLE, 0.57f, 0.95f)
        )
    )

    val CrossedArms = PoseTemplate(
        id = "crossed_arms",
        name = "Crossed Arms",
        description = "Confidence posture with folded arms",
        points = listOf(
            TemplatePoint(LandmarkType.NOSE, 0.50f, 0.22f),
            TemplatePoint(LandmarkType.LEFT_EAR, 0.46f, 0.19f),
            TemplatePoint(LandmarkType.RIGHT_EAR, 0.54f, 0.19f),
            TemplatePoint(LandmarkType.LEFT_SHOULDER, 0.39f, 0.32f),
            TemplatePoint(LandmarkType.RIGHT_SHOULDER, 0.61f, 0.32f),
            TemplatePoint(LandmarkType.LEFT_ELBOW, 0.35f, 0.49f),
            TemplatePoint(LandmarkType.RIGHT_ELBOW, 0.65f, 0.49f),
            TemplatePoint(LandmarkType.LEFT_WRIST, 0.53f, 0.47f),
            TemplatePoint(LandmarkType.RIGHT_WRIST, 0.47f, 0.47f),
            TemplatePoint(LandmarkType.LEFT_HIP, 0.42f, 0.62f),
            TemplatePoint(LandmarkType.RIGHT_HIP, 0.58f, 0.62f),
            TemplatePoint(LandmarkType.LEFT_KNEE, 0.42f, 0.79f),
            TemplatePoint(LandmarkType.RIGHT_KNEE, 0.58f, 0.79f),
            TemplatePoint(LandmarkType.LEFT_ANKLE, 0.42f, 0.95f),
            TemplatePoint(LandmarkType.RIGHT_ANKLE, 0.58f, 0.95f)
        )
    )

    val all = listOf(Standing, Seated, Candid, CrossedArms)
}
