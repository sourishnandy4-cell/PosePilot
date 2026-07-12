package com.posepilot.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class SettingsRepositoryTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        @Suppress("UNCHECKED_CAST")
        dataStore = mock(DataStore::class.java) as DataStore<Preferences>
    }

    @Test
    fun testDefaultSettingsWhenKeysAreNull() = runTest {
        val mockPreferences = androidx.datastore.preferences.core.preferencesOf()
        `when`(dataStore.data).thenReturn(flowOf(mockPreferences))
        repository = SettingsRepository(dataStore)
        
        val sensitivity = repository.sensitivity.first()
        val showGrid = repository.showGrid.first()
        val showHorizon = repository.showHorizon.first()
        val audioEnabled = repository.audioEnabled.first()
        val smartShutterEnabled = repository.smartShutterEnabled.first()
        val smartShutterDuration = repository.smartShutterDuration.first()

        assertEquals(1.0f, sensitivity)
        assertTrue(showGrid)
        assertTrue(showHorizon)
        assertTrue(audioEnabled)
        assertTrue(smartShutterEnabled)
        assertEquals(3, smartShutterDuration)
    }
}
