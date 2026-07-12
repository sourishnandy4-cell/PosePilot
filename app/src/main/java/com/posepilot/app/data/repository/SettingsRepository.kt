package com.posepilot.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val SHOW_GRID = booleanPreferencesKey("show_grid")
        val SHOW_HORIZON = booleanPreferencesKey("show_horizon")
        val AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        val SMART_SHUTTER_ENABLED = booleanPreferencesKey("smart_shutter_enabled")
        val SMART_SHUTTER_DURATION = intPreferencesKey("smart_shutter_duration")
    }

    val sensitivity: Flow<Float> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SENSITIVITY] ?: 1.0f
    }

    val showGrid: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_GRID] ?: true
    }

    val showHorizon: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_HORIZON] ?: true
    }

    val audioEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUDIO_ENABLED] ?: true
    }

    val smartShutterEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SMART_SHUTTER_ENABLED] ?: true
    }

    val smartShutterDuration: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SMART_SHUTTER_DURATION] ?: 3
    }

    suspend fun updateSensitivity(value: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SENSITIVITY] = value
        }
    }

    suspend fun updateShowGrid(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_GRID] = value
        }
    }

    suspend fun updateShowHorizon(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_HORIZON] = value
        }
    }

    suspend fun updateAudioEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_ENABLED] = value
        }
    }

    suspend fun updateSmartShutterEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMART_SHUTTER_ENABLED] = value
        }
    }

    suspend fun updateSmartShutterDuration(value: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMART_SHUTTER_DURATION] = value
        }
    }
}
