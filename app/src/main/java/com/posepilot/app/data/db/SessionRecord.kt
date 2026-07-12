package com.posepilot.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_records")
data class SessionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val averageScore: Int,
    val poseTemplateId: String?,
    val correctionCount: Int
)
