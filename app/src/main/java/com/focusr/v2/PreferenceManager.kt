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
        
        // New rule-based storage
        private val BLOCKING_RULES_KEY = stringPreferencesKey("blocking_rules_json")
        private val MIGRATION_COMPLETED_KEY = booleanPreferencesKey("migration_completed")

         // Pause state
        private val PAUSE_UNTIL_KEY = longPreferencesKey("pause_until_timestamp")
        
        // First launch detection
        private val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch_completed")

        
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

    // ========== PAUSE STATE MANAGEMENT ==========
    
    /**
     * Timestamp until which all rules are paused (null = not paused)
     */
    val pauseUntil: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[PAUSE_UNTIL_KEY]
    }
    
    /**
     * Check if this is the first launch of the app
     */
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        !(preferences[FIRST_LAUNCH_KEY] ?: false)
    }
    
    /**
     * Set pause until timestamp (null to unpause)
     */
    suspend fun setPauseUntil(timestamp: Long?) {
        context.dataStore.edit { preferences ->
            if (timestamp != null) {
                preferences[PAUSE_UNTIL_KEY] = timestamp
            } else {
                preferences.remove(PAUSE_UNTIL_KEY)
            }
        }
        Log.d("PreferencesManager", "Pause state updated: $timestamp")
    }
    
    /**
     * Mark first launch as completed
     */
    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = true
        }
        Log.d("PreferencesManager", "First launch completed")
    }
    
/**
 * Migration from old system (one-time operation)
 * This can be removed after all users have migrated
 */
suspend fun migrateOldBlockedApps() {
    val migrationCompleted = context.dataStore.data
        .map { it[MIGRATION_COMPLETED_KEY] ?: false }
        .first()
    
    if (migrationCompleted) {
        Log.d("PreferencesManager", "Migration already completed, skipping")
        return
    }
    
    Log.d("PreferencesManager", "Migration completed - no old data to migrate")
    
    // Mark migration as complete
    context.dataStore.edit { preferences ->
        preferences[MIGRATION_COMPLETED_KEY] = true
    }
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
                
                // SIMPLE rule fields (duration-based)
                rule.durationMinutes?.let {
                    put("durationMinutes", it)
                }
                rule.activatedAt?.let {
                    put("activatedAt", it)
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
                
                // MENTAL_CLARITY rule fields
                rule.windDownTime?.let {
                    put("windDownHour", it.first)
                    put("windDownMinute", it.second)
                }
                put("windDownEnabled", rule.windDownEnabled)
                rule.morningBlockDuration?.let {
                    put("morningBlockDuration", it)
                }
                put("morningFuryEnabled", rule.morningFuryEnabled)
                put("sleepDetectionMinutes", rule.sleepDetectionMinutes)
                put("morningWindowStart", rule.morningWindowStart)
                put("morningWindowEnd", rule.morningWindowEnd)
                rule.wakeUpDetectedAt?.let {
                    put("wakeUpDetectedAt", it)
                }
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
                
                // SIMPLE rule fields (duration-based)
                val durationMinutes = if (jsonObject.has("durationMinutes")) {
                    jsonObject.getInt("durationMinutes")
                } else null
                
                val activatedAt = if (jsonObject.has("activatedAt")) {
                    jsonObject.getLong("activatedAt")
                } else null
                
                // SCHEDULED rule fields
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
                
                // Load multi-app fields with backward compatibility
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
                    durationMinutes = durationMinutes,
                    activatedAt = activatedAt,
                    fromTime = fromTime,
                    toTime = toTime,
                    daysOfWeek = daysOfWeek,
                    // MENTAL_CLARITY fields
                    windDownTime = if (jsonObject.has("windDownHour")) {
                        Pair(jsonObject.getInt("windDownHour"), jsonObject.getInt("windDownMinute"))
                    } else null,
                    windDownEnabled = jsonObject.optBoolean("windDownEnabled", true),
                    morningBlockDuration = if (jsonObject.has("morningBlockDuration")) {
                        jsonObject.getInt("morningBlockDuration")
                    } else null,
                    morningFuryEnabled = jsonObject.optBoolean("morningFuryEnabled", true),
                    sleepDetectionMinutes = jsonObject.optInt("sleepDetectionMinutes", 300),
                    morningWindowStart = jsonObject.optInt("morningWindowStart", 4),
                    morningWindowEnd = jsonObject.optInt("morningWindowEnd", 12),
                    wakeUpDetectedAt = if (jsonObject.has("wakeUpDetectedAt")) {
                        jsonObject.getLong("wakeUpDetectedAt")
                    } else null
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