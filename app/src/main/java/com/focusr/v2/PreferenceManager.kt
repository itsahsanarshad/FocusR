package com.focusr.v2
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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

}