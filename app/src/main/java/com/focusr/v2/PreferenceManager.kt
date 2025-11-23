package com.focusr.v2
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.focusr.v2.models.BlockingRule
import com.focusr.v2.models.DayOfWeek
import com.focusr.v2.models.RuleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "blocker_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        // Old keys - kept for migration
        private val ADVANCED_MODE_KEY = booleanPreferencesKey("advanced_mode")
        private val FROM_HOUR_KEY = intPreferencesKey("from_hour")
        private val FROM_MINUTE_KEY = intPreferencesKey("from_minute")
        private val TO_HOUR_KEY = intPreferencesKey("to_hour")
        private val TO_MINUTE_KEY = intPreferencesKey("to_minute")
        private val BLOCKED_APPS_KEY = stringSetPreferencesKey("blocked_apps")
        private val BLOCKING_ENABLED_KEY = booleanPreferencesKey("blocking_enabled")
        private val BLOCKING_START_TIME_KEY = longPreferencesKey("blocking_start_time")
        private val WAS_BLOCKING_ENABLED_BEFORE_REBOOT_KEY = booleanPreferencesKey("was_blocking_enabled_before_reboot")
        
        // New rule-based storage
        private val BLOCKING_RULES_KEY = stringPreferencesKey("blocking_rules_json")
        private val MIGRATION_COMPLETED_KEY = booleanPreferencesKey("migration_completed")
    }

    // ========== OLD SYSTEM (kept for backward compatibility) ==========
    
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

    val wasBlockingEnabledBeforeReboot: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WAS_BLOCKING_ENABLED_BEFORE_REBOOT_KEY] ?: false
    }

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

    suspend fun setWasBlockingEnabledBeforeReboot(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WAS_BLOCKING_ENABLED_BEFORE_REBOOT_KEY] = enabled
        }
    }

    // ========== NEW RULE-BASED SYSTEM ==========
    
    /**
     * Flow of all blocking rules stored as JSON
     */
    val blockingRules: Flow<List<BlockingRule>> = context.dataStore.data.map { preferences ->
        val json = preferences[BLOCKING_RULES_KEY] ?: "[]"
        deserializeRules(json)
    }
    
    /**
     * Check if migration from old system has been completed
     */
    private val migrationCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MIGRATION_COMPLETED_KEY] ?: false
    }
    
    /**
     * Add a new blocking rule
     */
    suspend fun addRule(rule: BlockingRule) {
        if (!rule.isValid()) {
            Log.e("PreferencesManager", "Attempted to add invalid rule: $rule")
            return
        }
        
        context.dataStore.edit { preferences ->
            val currentRules = deserializeRules(preferences[BLOCKING_RULES_KEY] ?: "[]")
            val updatedRules = currentRules + rule
            preferences[BLOCKING_RULES_KEY] = serializeRules(updatedRules)
        }
        Log.d("PreferencesManager", "Added rule: ${rule.id} for ${rule.packageName}")
    }
    
    /**
     * Update an existing blocking rule
     */
    suspend fun updateRule(rule: BlockingRule) {
        if (!rule.isValid()) {
            Log.e("PreferencesManager", "Attempted to update with invalid rule: $rule")
            return
        }
        
        context.dataStore.edit { preferences ->
            val currentRules = deserializeRules(preferences[BLOCKING_RULES_KEY] ?: "[]")
            val updatedRules = currentRules.map { if (it.id == rule.id) rule else it }
            preferences[BLOCKING_RULES_KEY] = serializeRules(updatedRules)
        }
        Log.d("PreferencesManager", "Updated rule: ${rule.id}")
    }
    
    /**
     * Remove a blocking rule by ID
     */
    suspend fun removeRule(ruleId: String) {
        context.dataStore.edit { preferences ->
            val currentRules = deserializeRules(preferences[BLOCKING_RULES_KEY] ?: "[]")
            val updatedRules = currentRules.filter { it.id != ruleId }
            preferences[BLOCKING_RULES_KEY] = serializeRules(updatedRules)
        }
        Log.d("PreferencesManager", "Removed rule: $ruleId")
    }
    
    /**
     * Get all rules for a specific app
     */
    suspend fun getRulesForApp(packageName: String): List<BlockingRule> {
        return blockingRules.first().filter { it.packageName == packageName }
    }
    
    /**
     * Get all currently active (enabled) rules
     */
    suspend fun getAllActiveRules(): List<BlockingRule> {
        return blockingRules.first().filter { it.enabled }
    }
    
    /**
     * Migrate old blocked apps to new rule system
     */
    suspend fun migrateOldBlockedApps() {
        // Check if migration already done
        if (migrationCompleted.first()) {
            Log.d("PreferencesManager", "Migration already completed, skipping")
            return
        }
        
        val oldBlockedApps = blockedApps.first()
        if (oldBlockedApps.isEmpty()) {
            // No old data to migrate
            context.dataStore.edit { it[MIGRATION_COMPLETED_KEY] = true }
            Log.d("PreferencesManager", "No old data to migrate")
            return
        }
        
        Log.d("PreferencesManager", "Starting migration of ${oldBlockedApps.size} apps")
        
        val advancedMode = advancedMode.first()
        val fromTime = fromTime.first()
        val toTime = toTime.first()
        
        oldBlockedApps.forEach { packageName ->
            val rule = if (advancedMode) {
                // Convert to SCHEDULED rule
                BlockingRule(
                    packageName = packageName,
                    ruleType = RuleType.SCHEDULED,
                    fromTime = fromTime,
                    toTime = toTime,
                    daysOfWeek = DayOfWeek.values().toSet()
                )
            } else {
                // Convert to SIMPLE rule
                BlockingRule(
                    packageName = packageName,
                    ruleType = RuleType.SIMPLE,
                    blockUntilTime = toTime
                )
            }
            addRule(rule)
        }
        
        // Clear old data and mark migration complete
        context.dataStore.edit { preferences ->
            preferences[MIGRATION_COMPLETED_KEY] = true
            preferences.remove(BLOCKED_APPS_KEY)
        }
        
        Log.d("PreferencesManager", "Migration completed successfully")
    }
    
    // ========== JSON SERIALIZATION ==========
    
    private fun serializeRules(rules: List<BlockingRule>): String {
        val jsonArray = JSONArray()
        rules.forEach { rule ->
            val jsonObject = JSONObject().apply {
                put("id", rule.id)
                // NEW: Multi-app support
put("name", rule.name)
val appsArray = JSONArray()
rule.getApps().forEach { pkg -> appsArray.put(pkg) }
put("packageNames", appsArray)
// Keep old field for backward compatibility
@Suppress("DEPRECATION")
put("packageName", rule.packageName)
put("ruleType", rule.ruleType.name)
                put("enabled", rule.enabled)
                put("createdAt", rule.createdAt)
                
                // SIMPLE rule fields
                rule.blockUntilTime?.let {
                    put("blockUntilHour", it.first)
                    put("blockUntilMinute", it.second)
                }
                
                // SCHEDULED rule fields
                rule.fromTime?.let {
                    put("fromHour", it.first)
                    put("fromMinute", it.second)
                }
                rule.toTime?.let {
                    put("toHour", it.first)
                    put("toMinute", it.second)
                }
                
                // Days of week
                val daysArray = JSONArray()
                rule.daysOfWeek.forEach { day -> daysArray.put(day.name) }
                put("daysOfWeek", daysArray)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }
    
    private fun deserializeRules(json: String): List<BlockingRule> {
        return try {
            val jsonArray = JSONArray(json)
            val rules = mutableListOf<BlockingRule>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                val ruleType = RuleType.valueOf(jsonObject.getString("ruleType"))
                
                val blockUntilTime = if (jsonObject.has("blockUntilHour")) {
                    Pair(jsonObject.getInt("blockUntilHour"), jsonObject.getInt("blockUntilMinute"))
                } else null
                
                val fromTime = if (jsonObject.has("fromHour")) {
                    Pair(jsonObject.getInt("fromHour"), jsonObject.getInt("fromMinute"))
                } else null
                
                val toTime = if (jsonObject.has("toHour")) {
                    Pair(jsonObject.getInt("toHour"), jsonObject.getInt("toMinute"))
                } else null
                
                val daysOfWeek = if (jsonObject.has("daysOfWeek")) {
                    val daysArray = jsonObject.getJSONArray("daysOfWeek")
                    val days = mutableSetOf<DayOfWeek>()
                    for (j in 0 until daysArray.length()) {
                        days.add(DayOfWeek.valueOf(daysArray.getString(j)))
                    }
                    days
                } else DayOfWeek.values().toSet()
                
                // NEW: Load multi-app fields with backward compatibility
val name = if (jsonObject.has("name")) {
    jsonObject.getString("name")
} else ""
val packageNames = if (jsonObject.has("packageNames")) {
    val appsArray = jsonObject.getJSONArray("packageNames")
    (0 until appsArray.length()).map { appsArray.getString(it) }
} else if (jsonObject.has("packageName")) {
    listOf(jsonObject.getString("packageName"))
} else emptyList()
val rule = BlockingRule(
    id = jsonObject.getString("id"),
    name = name,
    packageNames = packageNames,
    ruleType = ruleType,
                    enabled = jsonObject.getBoolean("enabled"),
                    createdAt = jsonObject.getLong("createdAt"),
                    blockUntilTime = blockUntilTime,
                    fromTime = fromTime,
                    toTime = toTime,
                    daysOfWeek = daysOfWeek
                )
                
                rules.add(rule)
            }
            
            rules
        } catch (e: Exception) {
            Log.e("PreferencesManager", "Error deserializing rules: ${e.message}")
            emptyList()
        }
    }

}