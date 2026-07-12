package com.posepilot.app.domain.model

data class PostureIssue(
    val type: IssueType,
    val severity: Severity,
    val message: String,          // human-facing coaching copy
    val nudgeDirection: NudgeDirection? = null
)

enum class IssueType {
    SHOULDER_TILT,
    HEAD_TILT,
    SPINE_LEAN,
    FRAME_POSITION,
    SUBJECT_SCALE
}

enum class Severity {
    INFO,
    MINOR,
    NEEDS_CORRECTION
}

enum class NudgeDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN,
    CLOSER,
    FARTHER
}
