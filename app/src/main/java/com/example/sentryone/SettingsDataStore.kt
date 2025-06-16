package com.example.sentryone

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// At the top level of your file (outside any class)
val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

// Keys for your settings
object AppSettingsKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val LOCATION_ACCESS = booleanPreferencesKey("location_access")
    val TRIGGERING_MODE = booleanPreferencesKey("triggering_mode")
    val SILENTLY_SEND = booleanPreferencesKey("silently_send")
    val EMERGENCY_MESSAGE = stringPreferencesKey("emergency_message")
    val SHOW_DIALOGUE = booleanPreferencesKey("show_dialogue")
    val SHAKE_DETECTION = booleanPreferencesKey("shake_detection")
    val FLASH_TRIGGER = booleanPreferencesKey("flash_trigger")
    val HEPTIC_FEEDBACK = booleanPreferencesKey("heptic_feedback")
}

// Data class to represent your settings
data class AppSettings(
    val darkMode: Boolean = false,
    val locationAccess: Boolean = false,
    val triggeringMode: Boolean = false,
    val silentlySend: Boolean = false,
    val emergencyMessage: String = "",
    val showDialogue: Boolean = false,
    val shakeDetection: Boolean = false,
    val flashTrigger: Boolean = false,
    val hepticFeedback: Boolean = false

)

// Helper class to encapsulate DataStore operations
class AppSettingsManager(private val context: Context) {

    val appSettingsFlow: Flow<AppSettings> = context.appSettingsDataStore.data
        .map { preferences ->
            AppSettings(
                darkMode = preferences[AppSettingsKeys.DARK_MODE] ?: false,
                locationAccess = preferences[AppSettingsKeys.LOCATION_ACCESS] ?: false,
                triggeringMode = preferences[AppSettingsKeys.TRIGGERING_MODE] ?: false,
                silentlySend = preferences[AppSettingsKeys.SILENTLY_SEND] ?: false,
                emergencyMessage = preferences[AppSettingsKeys.EMERGENCY_MESSAGE] ?: "",
                showDialogue = preferences[AppSettingsKeys.SHOW_DIALOGUE] ?: false,
                shakeDetection = preferences[AppSettingsKeys.SHAKE_DETECTION] ?: false,
                flashTrigger = preferences[AppSettingsKeys.FLASH_TRIGGER] ?: false, // Mapped flashTrigger
                hepticFeedback = preferences[AppSettingsKeys.HEPTIC_FEEDBACK] ?: false // Mapped hepticFeedback

            )
        }

    suspend fun updateSetting(key: Preferences.Key<Boolean>, value: Boolean) {
        context.appSettingsDataStore.edit { settings ->
            settings[key] = value
        }
    }

    suspend fun updateEmergencyMessage(message: String) {
        context.appSettingsDataStore.edit { settings ->
            settings[AppSettingsKeys.EMERGENCY_MESSAGE] = message
        }
    }

    suspend fun getBooleanSetting(key: Preferences.Key<Boolean>, defaultValue: Boolean): Boolean {
        return context.appSettingsDataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }.first() // Use .first() to get the current value once
    }
}