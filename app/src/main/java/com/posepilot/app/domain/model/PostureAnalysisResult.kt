package com.posepilot.app.domain.model

data class PostureAnalysisResult(
    val overallScore: Int,               // 0..100 composite score
    val issues: List<PostureIssue>,      // sorted by severity, worst first
    val timestampMs: Long
)
