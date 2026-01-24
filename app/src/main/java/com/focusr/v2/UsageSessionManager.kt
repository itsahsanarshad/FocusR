package com.focusr.v2

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject

/**
 * Manages in-memory session tracking for Smart Cooldown rules.
 * Tracks continuous usage time per app and manages cooldown states.
 * 
 * IMPORTANT: Cooldown states are persisted to SharedPreferences to survive
 * service restarts. Session usage tracking is still in-memory (acceptable
 * since it resets on service restart anyway - user gets a fresh session).
 */
class UsageSessionManager(context: Context) {
    
    companion object {
        private const val TAG = "UsageSessionManager"
        private const val PREFS_NAME = "smart_cooldown_prefs"
        private const val KEY_COOLDOWNS = "active_cooldowns"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // In-memory storage for app sessions (usage tracking)
    private val sessions = mutableMapOf<String, AppSession>()
    
    // Persisted cooldowns (survives service restart)
    private val persistedCooldowns = mutableMapOf<String, Long>()  // packageName -> cooldownEndsAt
    
    init {
        // Load persisted cooldowns on startup
        loadPersistedCooldowns()
    }
    
    /**
     * Represents an active usage session for an app
     */
    data class AppSession(
        val packageName: String,
        var sessionStartTime: Long = System.currentTimeMillis(),
        var totalUsageSeconds: Int = 0,
        var lastSeenTime: Long = System.currentTimeMillis(),
        var lastClosedAt: Long? = null  // For post-closure break tracking
    )
    
    /**
     * Updates session tracking when an app is in foreground.
     * Called every 3 seconds from the monitoring service.
     * 
     * @param packageName The app currently in foreground
     * @param sessionResetMinutes Minutes of inactivity before session resets
     * @return The current session for this app
     */
    fun updateSession(packageName: String, sessionResetMinutes: Int): AppSession {
        val now = System.currentTimeMillis()
        val session = sessions.getOrPut(packageName) { AppSession(packageName) }
        
        // Check if this is a new session (app was away longer than reset threshold)
        val timeSinceLastSeen = now - session.lastSeenTime
        val resetThresholdMs = sessionResetMinutes * 60 * 1000L
        
        if (timeSinceLastSeen > resetThresholdMs) {
            // Start a new session
            Log.d(TAG, "Starting new session for $packageName (was away ${timeSinceLastSeen / 1000}s)")
            session.sessionStartTime = now
            session.totalUsageSeconds = 0
        } else {
            // Continue existing session - add ~3 seconds (monitoring interval)
            session.totalUsageSeconds += 3
        }
        
        session.lastSeenTime = now
        
        Log.d(TAG, "$packageName: ${session.totalUsageSeconds}s this session")
        return session
    }
    
    /**
     * Gets the current session duration in seconds for an app.
     */
    fun getSessionDurationSeconds(packageName: String): Int {
        return sessions[packageName]?.totalUsageSeconds ?: 0
    }
    
    /**
     * Gets the current session duration in minutes for an app.
     */
    fun getSessionDurationMinutes(packageName: String): Int {
        return getSessionDurationSeconds(packageName) / 60
    }
    
    /**
     * Checks if an app is currently in cooldown (uses persisted storage).
     */
    fun isInCooldown(packageName: String): Boolean {
        val cooldownEnd = persistedCooldowns[packageName] ?: return false
        
        // Check if cooldown has expired
        if (System.currentTimeMillis() >= cooldownEnd) {
            // Cooldown expired - clear it
            clearCooldown(packageName)
            Log.d(TAG, "Cooldown expired for $packageName")
            return false
        }
        
        Log.d(TAG, "$packageName is in cooldown until ${cooldownEnd}")
        return true
    }
    
    /**
     * Gets remaining cooldown time in minutes.
     */
    fun getCooldownRemainingMinutes(packageName: String): Int? {
        val cooldownEnd = persistedCooldowns[packageName] ?: return null
        
        val remaining = (cooldownEnd - System.currentTimeMillis()) / 1000 / 60
        return remaining.toInt().coerceAtLeast(0)
    }
    
    /**
     * Starts a cooldown period for an app (persisted to SharedPreferences).
     */
    fun startCooldown(packageName: String, cooldownMinutes: Int) {
        val cooldownEndsAt = System.currentTimeMillis() + (cooldownMinutes * 60 * 1000L)
        persistedCooldowns[packageName] = cooldownEndsAt
        savePersistedCooldowns()
        
        // Also reset session usage for when cooldown ends
        sessions[packageName]?.totalUsageSeconds = 0
        
        Log.d(TAG, "Started ${cooldownMinutes}m cooldown for $packageName (persisted)")
    }
    
    /**
     * Clears cooldown for an app.
     */
    private fun clearCooldown(packageName: String) {
        persistedCooldowns.remove(packageName)
        savePersistedCooldowns()
        
        // Reset session for fresh start
        sessions[packageName]?.totalUsageSeconds = 0
    }
    
    /**
     * Records when an app was closed (for post-closure break tracking).
     */
    fun recordAppClosed(packageName: String) {
        val session = sessions[packageName] ?: return
        session.lastClosedAt = System.currentTimeMillis()
        Log.d(TAG, "Recorded $packageName closed")
    }
    
    /**
     * Checks if an app is still in post-closure break period.
     */
    fun isInPostClosureBreak(packageName: String, breakMinutes: Int): Boolean {
        val session = sessions[packageName] ?: return false
        val closedAt = session.lastClosedAt ?: return false
        
        val breakEndTime = closedAt + (breakMinutes * 60 * 1000L)
        return System.currentTimeMillis() < breakEndTime
    }
    
    /**
     * Gets remaining post-closure break time in minutes.
     */
    fun getPostClosureBreakRemainingMinutes(packageName: String, breakMinutes: Int): Int? {
        val session = sessions[packageName] ?: return null
        val closedAt = session.lastClosedAt ?: return null
        
        val breakEndTime = closedAt + (breakMinutes * 60 * 1000L)
        val remaining = (breakEndTime - System.currentTimeMillis()) / 1000 / 60
        return if (remaining > 0) remaining.toInt() else null
    }
    
    /**
     * Resets session for an app (e.g., when rule is disabled).
     */
    fun resetSession(packageName: String) {
        sessions.remove(packageName)
        clearCooldown(packageName)
        Log.d(TAG, "Reset session for $packageName")
    }
    
    /**
     * Clears all sessions.
     */
    fun clearAllSessions() {
        sessions.clear()
        persistedCooldowns.clear()
        savePersistedCooldowns()
        Log.d(TAG, "Cleared all sessions")
    }
    
    /**
     * Checks if a warning should be shown (X minutes before limit).
     */
    fun shouldShowWarning(packageName: String, maxUsageMinutes: Int, warnBeforeMinutes: Int): Boolean {
        val usedMinutes = getSessionDurationMinutes(packageName)
        val warningThreshold = maxUsageMinutes - warnBeforeMinutes
        
        // Show warning when exactly at threshold (to avoid repeated warnings)
        return usedMinutes == warningThreshold
    }
    
    // ========== Persistence Methods ==========
    
    /**
     * Loads persisted cooldowns from SharedPreferences.
     */
    private fun loadPersistedCooldowns() {
        try {
            val json = prefs.getString(KEY_COOLDOWNS, null) ?: return
            val jsonObject = JSONObject(json)
            
            val now = System.currentTimeMillis()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val packageName = keys.next()
                val cooldownEnd = jsonObject.getLong(packageName)
                
                // Only load cooldowns that haven't expired
                if (cooldownEnd > now) {
                    persistedCooldowns[packageName] = cooldownEnd
                    Log.d(TAG, "Loaded persisted cooldown for $packageName (${(cooldownEnd - now) / 1000 / 60}m remaining)")
                }
            }
            
            Log.d(TAG, "Loaded ${persistedCooldowns.size} persisted cooldowns")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading persisted cooldowns: ${e.message}")
        }
    }
    
    /**
     * Saves persisted cooldowns to SharedPreferences.
     */
    private fun savePersistedCooldowns() {
        try {
            val jsonObject = JSONObject()
            persistedCooldowns.forEach { (packageName, cooldownEnd) ->
                jsonObject.put(packageName, cooldownEnd)
            }
            
            prefs.edit().putString(KEY_COOLDOWNS, jsonObject.toString()).apply()
            Log.d(TAG, "Saved ${persistedCooldowns.size} cooldowns to SharedPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving persisted cooldowns: ${e.message}")
        }
    }
}

