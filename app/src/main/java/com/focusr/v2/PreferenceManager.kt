package com.focusr.v2
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.focusr.v2.data.TimerFeature
import com.focusr.v2.data.TimerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "blocker_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val ADVANCED_MODE_KEY = booleanPreferencesKey("advanced_mode")
        private val FROM_HOUR_KEY = intPreferencesKey("from_hour")
        private val FROM_MINUTE_KEY = intPreferencesKey("from_minute")
        private val TO_HOUR_KEY = intPreferencesKey("to_hour")
        private val TO_MINUTE_KEY = intPreferencesKey("to_minute")
        private val BLOCKED_APPS_KEY = stringSetPreferencesKey("blocked_apps")
        private val BLOCKING_ENABLED_KEY = booleanPreferencesKey("blocking_enabled")
        private val BLOCKING_START_TIME_KEY = longPreferencesKey("blocking_start_time")
        private val WAS_BLOCKING_ENABLED_BEFORE_REBOOT_KEY = booleanPreferencesKey("was_blocking_enabled_before_reboot")


        // New feature system keys
        private val ENABLED_FEATURES_KEY = stringSetPreferencesKey("enabled_features")
        private val DAILY_LIMIT_MINUTES_KEY = intPreferencesKey("daily_limit_minutes")
        private val BREAK_INTERVAL_MINUTES_KEY = intPreferencesKey("break_interval_minutes")
        private val WEEKEND_ENABLED_KEY = booleanPreferencesKey("weekend_enabled")
        private val SMART_SCHEDULING_ENABLED_KEY = booleanPreferencesKey("smart_scheduling_enabled")
        private val DAILY_USAGE_MINUTES_KEY = intPreferencesKey("daily_usage_minutes")
        private val LAST_USAGE_DATE_KEY = stringPreferencesKey("last_usage_date")

        // New keys for simple mode timer
        private val SIMPLE_MODE_DURATION_MINUTES_KEY = intPreferencesKey("simple_mode_duration_minutes")
        private val SIMPLE_MODE_SESSION_START_TIME_KEY = longPreferencesKey("simple_mode_session_start_time")


    }

    // Flow for reading preferences
    val advancedMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ADVANCED_MODE_KEY] ?: false
    }

    val fromTime: Flow<Pair<Int, Int>> = context.dataStore.data.map { preferences ->
        Pair(
            preferences[FROM_HOUR_KEY] ?: 8,
            preferences[FROM_MINUTE_KEY] ?: 0
        )
    }

    val toTime: Flow<Pair<Int, Int>> = context.dataStore.data.map { preferences ->
        Pair(
            preferences[TO_HOUR_KEY] ?: 22,
            preferences[TO_MINUTE_KEY] ?: 0
        )
    }

    val blockedApps: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[BLOCKED_APPS_KEY] ?: emptySet()
    }

    val blockingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BLOCKING_ENABLED_KEY] ?: false
    }


    val blockingStartTime: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[BLOCKING_START_TIME_KEY]
    }

    // Add this flow to read the boot restore flag
    val wasBlockingEnabledBeforeReboot: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WAS_BLOCKING_ENABLED_BEFORE_REBOOT_KEY] ?: false
    }

    // New flows for feature system
    val timerSettings: Flow<TimerSettings> = context.dataStore.data.map { preferences ->
        val featuresSet = preferences[ENABLED_FEATURES_KEY]
            ?.mapNotNull { featureName ->
                try { TimerFeature.valueOf(featureName) } catch (e: Exception) { null }
            }?.toSet() ?: setOf(TimerFeature.SIMPLE_MODE)

        TimerSettings(
            enabledFeatures = featuresSet,
            dailyLimitMinutes = preferences[DAILY_LIMIT_MINUTES_KEY] ?: 480,
            breakIntervalMinutes = preferences[BREAK_INTERVAL_MINUTES_KEY] ?: 25,
//            weekendEnabled = preferences[WEEKEND_ENABLED_KEY] ?: true,
//            smartSchedulingEnabled = preferences[SMART_SCHEDULING_ENABLED_KEY] ?: false
        )
    }

    val dailyUsageMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastUsageDate = preferences[LAST_USAGE_DATE_KEY] ?: ""

        if (lastUsageDate == today) {
            preferences[DAILY_USAGE_MINUTES_KEY] ?: 0
        } else {
            0 // Reset if it's a new day
        }
    }

    // Add these new flows after existing flows

    val simpleModeDurationMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SIMPLE_MODE_DURATION_MINUTES_KEY] ?: 60 // Default 1 hour
    }

    val simpleModeSessionStartTime: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[SIMPLE_MODE_SESSION_START_TIME_KEY]
    }

    // Functions for writing preferences
    suspend fun setAdvancedMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ADVANCED_MODE_KEY] = enabled
        }
    }

    suspend fun setFromTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[FROM_HOUR_KEY] = hour
            preferences[FROM_MINUTE_KEY] = minute
        }
    }

    suspend fun setToTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[TO_HOUR_KEY] = hour
            preferences[TO_MINUTE_KEY] = minute
        }
    }

    suspend fun setBlockedApps(apps: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[BLOCKED_APPS_KEY] = apps
        }
    }

    suspend fun setBlockingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BLOCKING_ENABLED_KEY] = enabled
        }
    }

    suspend fun setBlockingStartTime(timestamp: Long?) {
        context.dataStore.edit { preferences ->
            if (timestamp != null) {
                preferences[BLOCKING_START_TIME_KEY] = timestamp
            } else {
                preferences.remove(BLOCKING_START_TIME_KEY)
            }
        }
    }

    // Add this function to set the boot restore flag
    suspend fun setWasBlockingEnabledBeforeReboot(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WAS_BLOCKING_ENABLED_BEFORE_REBOOT_KEY] = enabled
        }
    }

    // Add these FUNCTIONS to your existing class
    suspend fun toggleFeature(feature: TimerFeature, enabled: Boolean) {
        val currentSettings = timerSettings.first()
        val newFeatures = if (enabled) {
            currentSettings.enabledFeatures + feature
        } else {
            currentSettings.enabledFeatures - feature
        }

        // Handle mutual exclusion between simple and advanced mode
        val finalFeatures = when (feature) {
            TimerFeature.SIMPLE_MODE -> {
                if (enabled) {
                    setAdvancedMode(false) // Sync with old system
                    newFeatures - TimerFeature.ADVANCED_MODE
                } else newFeatures
            }
            TimerFeature.ADVANCED_MODE -> {
                if (enabled) {
                    setAdvancedMode(true) // Sync with old system
                    newFeatures - TimerFeature.SIMPLE_MODE
                } else newFeatures
            }
            else -> newFeatures
        }

        context.dataStore.edit { preferences ->
            preferences[ENABLED_FEATURES_KEY] = finalFeatures.map { it.name }.toSet()
        }
    }

    suspend fun updateDailyUsage(additionalMinutes: Int) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        context.dataStore.edit { preferences ->
            val currentUsage = preferences[DAILY_USAGE_MINUTES_KEY] ?: 0
            preferences[DAILY_USAGE_MINUTES_KEY] = currentUsage + additionalMinutes
            preferences[LAST_USAGE_DATE_KEY] = today
        }
    }

    // Update the existing updateTimerSettings function to include simple mode duration
    suspend fun updateTimerSettings(settings: TimerSettings, simpleModeDurationMinutes: Int? = null) {
        context.dataStore.edit { preferences ->
            preferences[ENABLED_FEATURES_KEY] = settings.enabledFeatures.map { it.name }.toSet()
            preferences[DAILY_LIMIT_MINUTES_KEY] = settings.dailyLimitMinutes
            preferences[BREAK_INTERVAL_MINUTES_KEY] = settings.breakIntervalMinutes
//            preferences[WEEKEND_ENABLED_KEY] = settings.weekendEnabled

            // Add simple mode duration if provided
            simpleModeDurationMinutes?.let {
                preferences[SIMPLE_MODE_DURATION_MINUTES_KEY] = it
            }
        }
    }

    // Add these new functions after existing functions

    suspend fun setSimpleModeDurationMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[SIMPLE_MODE_DURATION_MINUTES_KEY] = minutes
        }
    }

    suspend fun setSimpleModeSessionStartTime(timestamp: Long?) {
        context.dataStore.edit { preferences ->
            if (timestamp != null) {
                preferences[SIMPLE_MODE_SESSION_START_TIME_KEY] = timestamp
            } else {
                preferences.remove(SIMPLE_MODE_SESSION_START_TIME_KEY)
            }
        }
    }
}

