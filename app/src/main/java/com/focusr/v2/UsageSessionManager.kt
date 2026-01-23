package com.focusr.v2

import android.util.Log

/**
 * Manages in-memory session tracking for Smart Cooldown rules.
 * Tracks continuous usage time per app and manages cooldown states.
 */
class UsageSessionManager {
    
    companion object {
        private const val TAG = "UsageSessionManager"
    }
    
    // In-memory storage for app sessions
    private val sessions = mutableMapOf<String, AppSession>()
    
    /**
     * Represents an active usage session for an app
     */
    data class AppSession(
        val packageName: String,
        var sessionStartTime: Long = System.currentTimeMillis(),
        var totalUsageSeconds: Int = 0,
        var lastSeenTime: Long = System.currentTimeMillis(),
        var inCooldown: Boolean = false,
        var cooldownEndsAt: Long? = null,
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
            session.inCooldown = false
            session.cooldownEndsAt = null
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
     * Checks if an app is currently in cooldown.
     */
    fun isInCooldown(packageName: String): Boolean {
        val session = sessions[packageName] ?: return false
        
        if (!session.inCooldown) return false
        
        // Check if cooldown has expired
        val cooldownEnd = session.cooldownEndsAt ?: return false
        if (System.currentTimeMillis() >= cooldownEnd) {
            // Cooldown expired - clear it
            session.inCooldown = false
            session.cooldownEndsAt = null
            session.totalUsageSeconds = 0  // Reset session for next use
            Log.d(TAG, "Cooldown expired for $packageName")
            return false
        }
        
        return true
    }
    
    /**
     * Gets remaining cooldown time in minutes.
     */
    fun getCooldownRemainingMinutes(packageName: String): Int? {
        val session = sessions[packageName] ?: return null
        val cooldownEnd = session.cooldownEndsAt ?: return null
        
        if (!session.inCooldown) return null
        
        val remaining = (cooldownEnd - System.currentTimeMillis()) / 1000 / 60
        return remaining.toInt().coerceAtLeast(0)
    }
    
    /**
     * Starts a cooldown period for an app.
     */
    fun startCooldown(packageName: String, cooldownMinutes: Int) {
        val session = sessions.getOrPut(packageName) { AppSession(packageName) }
        session.inCooldown = true
        session.cooldownEndsAt = System.currentTimeMillis() + (cooldownMinutes * 60 * 1000L)
        Log.d(TAG, "Started ${cooldownMinutes}m cooldown for $packageName")
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
        Log.d(TAG, "Reset session for $packageName")
    }
    
    /**
     * Clears all sessions.
     */
    fun clearAllSessions() {
        sessions.clear()
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
}
