package com.posepilot.app.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionRecordTest {

    @Test
    fun testSessionRecordProperties() {
        val record = SessionRecord(
            id = 10,
            timestampMs = 1719958000000L,
            averageScore = 92,
            poseTemplateId = "standing",
            correctionCount = 3
        )

        assertEquals(10L, record.id)
        assertEquals(1719958000000L, record.timestampMs)
        assertEquals(92, record.averageScore)
        assertEquals("standing", record.poseTemplateId)
        assertEquals(3, record.correctionCount)
    }
}
