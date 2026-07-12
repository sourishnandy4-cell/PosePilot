package com.posepilot.app.data.pose

import com.posepilot.app.domain.model.LandmarkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PoseLandmarkerHelperTest {

    @Test
    fun testLandmarkTypeFromIndex() {
        assertEquals(LandmarkType.NOSE, LandmarkType.fromIndex(0))
        assertEquals(LandmarkType.LEFT_SHOULDER, LandmarkType.fromIndex(11))
        assertEquals(LandmarkType.RIGHT_SHOULDER, LandmarkType.fromIndex(12))
        assertEquals(LandmarkType.LEFT_ANKLE, LandmarkType.fromIndex(27))
        assertEquals(LandmarkType.RIGHT_FOOT_INDEX, LandmarkType.fromIndex(32))
    }

    @Test
    fun testLandmarkTypeFromIndexOutOfBounds() {
        assertNull(LandmarkType.fromIndex(-1))
        assertNull(LandmarkType.fromIndex(33))
        assertNull(LandmarkType.fromIndex(100))
    }
}
