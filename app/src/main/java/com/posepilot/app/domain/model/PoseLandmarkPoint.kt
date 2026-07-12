package com.posepilot.app.domain.model

data class PoseLandmarkPoint(
    val type: LandmarkType,
    val x: Float,           // normalized 0..1 relative to width
    val y: Float,           // normalized 0..1 relative to height
    val z: Float,           // relative depth coordinates
    val visibility: Float   // visibility confidence 0..1
)
