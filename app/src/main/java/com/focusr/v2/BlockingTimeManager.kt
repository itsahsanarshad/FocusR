package com.focusr.v2

import android.util.Log
import kotlinx.coroutines.flow.first
import java.util.*

class BlockingTimeManager(private val preferencesManager: PreferencesManager) {

    // Flags for tracking state across different scenarios
    private var hasDayChanged = false
    private var hasBlockingStarted = false

    /**
     * Checks if the current time is within the blocking hours
     */
    suspend fun isInBlockingHours(): Boolean {
        val advancedMode = preferencesManager.advancedMode.first()
        val fromTime = preferencesManager.fromTime.first()
        val toTime = preferencesManager.toTime.first()

        val calendar = Calendar.getInstance()
        val currentTimeInMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val fromMinutes = fromTime.first * 60 + fromTime.second
        val toMinutes = toTime.first * 60 + toTime.second

        return if (!advancedMode) {
            currentTimeInMinutes <= toMinutes
        } else {
            if (fromMinutes > toMinutes) {
                // Overnight: 10PM to 2AM
                currentTimeInMinutes >= fromMinutes || currentTimeInMinutes <= toMinutes
            } else {
                // Same-day block
                currentTimeInMinutes in fromMinutes..toMinutes
            }
        }
    }

    /**
     * Checks if the blocking time has been exceeded and should stop
     */
    suspend fun hasExceededToTime(): Boolean {
        val advancedMode = preferencesManager.advancedMode.first()
        val fromTime = preferencesManager.fromTime.first()
        val toTime = preferencesManager.toTime.first()
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val fromMinutes = fromTime.first * 60 + fromTime.second
        val toMinutes = toTime.first * 60 + toTime.second

        Log.d("BlockingTimeManager", "fromMinutes $fromMinutes, toMinutes $toMinutes, currentMinutes $currentMinutes")

        if (!advancedMode) {
            // Simple mode: time has exceeded if current > toTime
            return currentMinutes > toMinutes
        }

        // Advanced mode: Handle all scenarios separately

        // Case 1: Overnight scenario (fromMinutes > toMinutes)
        if (fromMinutes > toMinutes) {
            Log.d("BlockingTimeManager", "Overnight scenario detected")

            // Reset hasDayChanged when we're back in the "from" period
            if (currentMinutes >= fromMinutes && hasDayChanged) {
                hasDayChanged = false
                Log.d("BlockingTimeManager", "Day changed flag reset - new cycle starting")
            }

            // Set hasDayChanged flag based on current time
            if (currentMinutes <= toMinutes && !hasDayChanged) {
                hasDayChanged = true
                Log.d("BlockingTimeManager", "Day changed flag set to true")
            }

            // Also set flag when exactly at midnight
            if (currentMinutes == 0 && !hasDayChanged) {
                hasDayChanged = true
                Log.d("BlockingTimeManager", "Day changed flag set to true at midnight")
            }

            // Time is exceeded only when we're past toTime AND day has changed
            if (hasDayChanged && currentMinutes > toMinutes) {
                Log.d("BlockingTimeManager", "Overnight session exceeded")
                return true
            } else {
                Log.d("BlockingTimeManager", "Overnight session active")
                return false
            }
        }

        // Case 2: Same day or Future block scenario (fromMinutes <= toMinutes)

        // Reset hasBlockingStarted when we're outside the blocking window
        if (currentMinutes < fromMinutes || currentMinutes > toMinutes) {
            if (hasBlockingStarted) {
                hasBlockingStarted = false
                Log.d("BlockingTimeManager", "Blocking session flag reset - outside blocking window")
                return true
            }
        }

        // Set blocking started flag when we reach fromTime
        if (currentMinutes >= fromMinutes && currentMinutes <= toMinutes && !hasBlockingStarted) {
            hasBlockingStarted = true
            Log.d("BlockingTimeManager", "Blocking session started")
        }

        // Case 2a: Blocking session has started
        if (hasBlockingStarted) {
            Log.d("BlockingTimeManager", "Checking if blocking session exceeded")
            if (currentMinutes > toMinutes) {
                Log.d("BlockingTimeManager", "Blocking session exceeded - stopping service")
                return true
            } else {
                Log.d("BlockingTimeManager", "Blocking session active")
                return false
            }
        }

        // Case 2b: Before blocking window or after blocking window
        Log.d("BlockingTimeManager", "Outside blocking window - no blocking active")
        return false
    }

    /**
     * Checks if we're currently in a blocking session (used for app startup)
     */
    suspend fun isCurrentlyInBlockingSession(): Boolean {
        val advancedMode = preferencesManager.advancedMode.first()
        val fromTime = preferencesManager.fromTime.first()
        val toTime = preferencesManager.toTime.first()

        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val fromMinutes = fromTime.first * 60 + fromTime.second
        val toMinutes = toTime.first * 60 + toTime.second

        return if (!advancedMode) {
            // In simple mode, check if current time is before end time
            currentMinutes <= toMinutes
        } else {
            // In advanced mode, handle both same-day and overnight scenarios
            if (fromMinutes > toMinutes) {
                // Overnight scenario: blocking spans midnight
                currentMinutes >= fromMinutes || currentMinutes <= toMinutes
            } else {
                // Same-day scenario: blocking within same day
                currentMinutes in fromMinutes..toMinutes
            }
        }
    }

    /**
     * Calculates the next start time for the blocking service
     */
    suspend fun calculateNextStartTime(): Long {
        val fromTime = preferencesManager.fromTime.first()
        val toTime = preferencesManager.toTime.first()

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val fromMinutes = fromTime.first * 60 + fromTime.second
        val toMinutes = toTime.first * 60 + toTime.second

        val startCalendar = Calendar.getInstance()

        // Check if we should start service now
        val shouldStartNow = if (fromMinutes > toMinutes) {
            // Overnight case (e.g., 10 PM to 2 AM)
            currentMinutes >= fromMinutes || currentMinutes <= toMinutes
        } else {
            // Same day case (e.g., 8 AM to 10 AM)
            currentMinutes in fromMinutes..toMinutes
        }

        if (shouldStartNow) {
            return System.currentTimeMillis() // Start immediately
        }

        // Schedule for next blocking period
        if (currentMinutes < fromMinutes) {
            // Start today at fromTime
            startCalendar.set(Calendar.HOUR_OF_DAY, fromTime.first)
            startCalendar.set(Calendar.MINUTE, fromTime.second)
            startCalendar.set(Calendar.SECOND, 0)
        } else {
            // Start tomorrow at fromTime
            startCalendar.add(Calendar.DAY_OF_YEAR, 1)
            startCalendar.set(Calendar.HOUR_OF_DAY, fromTime.first)
            startCalendar.set(Calendar.MINUTE, fromTime.second)
            startCalendar.set(Calendar.SECOND, 0)
        }

        return startCalendar.timeInMillis
    }

    /**
     * Gets a dynamic notification message based on current blocking state
     */
    suspend fun getDynamicNotificationMessage(): String {
        val advancedMode = preferencesManager.advancedMode.first()
        val fromTime = preferencesManager.fromTime.first()
        val toTime = preferencesManager.toTime.first()

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val fromMinutes = fromTime.first * 60 + fromTime.second
        val toMinutes = toTime.first * 60 + toTime.second

        fun formatTime(minutes: Int): String {
            val h = minutes / 60
            val m = minutes % 60
            return "${if (h > 0) "${h}h " else ""}${m}m"
        }

        if (!advancedMode) {
            if (currentMinutes <= toMinutes) {
                val remaining = toMinutes - currentMinutes
                return if (remaining <= 15) "Session ends in ${formatTime(remaining)}" else "Blocking active"
            } else {
                return "Blocking inactive"
            }
        }

        // advancedMode
        val isOvernight = fromMinutes > toMinutes
        val inBlock = if (isOvernight) {
            currentMinutes >= fromMinutes || currentMinutes <= toMinutes
        } else {
            currentMinutes in fromMinutes..toMinutes
        }

        return when {
            inBlock -> {
                val minutesLeft = if (currentMinutes <= toMinutes) {
                    toMinutes - currentMinutes
                } else {
                    (1440 - currentMinutes) + toMinutes
                }
                if (minutesLeft <= 15) "Session ends in ${formatTime(minutesLeft)}"
                else "Blocking active"
            }

            else -> {
                val minutesUntilStart = if (currentMinutes <= fromMinutes) {
                    fromMinutes - currentMinutes
                } else {
                    (1440 - currentMinutes) + fromMinutes
                }
                if (minutesUntilStart <= 15) "Session starts in ${formatTime(minutesUntilStart)}"
                else "Next session at ${fromTime.first.toString().padStart(2, '0')}:${fromTime.second.toString().padStart(2, '0')}"
            }
        }
    }

    /**
     * Gets a user-friendly message for when blocking is toggled
     */
    suspend fun getBlockingScheduleMessage(): String {
        val advancedMode = preferencesManager.advancedMode.first()
        val fromTime = preferencesManager.fromTime.first()
        val toTime = preferencesManager.toTime.first()

        if (!advancedMode) {
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val toMinutes = toTime.first * 60 + toTime.second

            return if (currentMinutes <= toMinutes) {
                "Blocking started until ${toTime.first}:${toTime.second.toString().padStart(2, '0')}"
            } else {
                "Blocking scheduled for tomorrow until ${toTime.first}:${toTime.second.toString().padStart(2, '0')}"
            }
        } else {
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val fromMinutes = fromTime.first * 60 + fromTime.second
            val toMinutes = toTime.first * 60 + toTime.second

            val isCurrentlyInBlockingTime = if (fromMinutes > toMinutes) {
                // Overnight case
                currentMinutes >= fromMinutes || currentMinutes <= toMinutes
            } else {
                // Same day case
                currentMinutes in fromMinutes..toMinutes
            }

            return if (isCurrentlyInBlockingTime) {
                "Blocking is now active until ${toTime.first}:${toTime.second.toString().padStart(2, '0')}"
            } else {
                "Blocking will start at ${fromTime.first}:${fromTime.second.toString().padStart(2, '0')}"
            }
        }
    }

    /**
     * Resets all internal flags (call this when service stops)
     */
    fun resetFlags() {
        hasDayChanged = false
        hasBlockingStarted = false
    }

    // ========== NEW RULE-BASED SYSTEM ==========
    
    /**
     * Check if a specific app should be blocked based on its rules
     */
    suspend fun shouldBlockApp(packageName: String): Boolean {
        val rules = preferencesManager.getRulesForApp(packageName)
        
        // App should be blocked if ANY of its enabled rules are currently active
        return rules.any { rule ->
            rule.enabled && isRuleActive(rule)
        }
    }
    
    /**
     * Check if a specific rule is currently active
     */
    private fun isRuleActive(rule: com.focusr.v2.models.BlockingRule): Boolean {
        return when (rule.ruleType) {
            com.focusr.v2.models.RuleType.SIMPLE -> checkSimpleRule(rule)
            com.focusr.v2.models.RuleType.SCHEDULED -> checkScheduledRule(rule)
        }
    }
    
    /**
     * Check if a SIMPLE rule is active (block until time today)
     */
    private fun checkSimpleRule(rule: com.focusr.v2.models.BlockingRule): Boolean {
        val blockUntilTime = rule.blockUntilTime ?: return false
        
        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val blockUntilMinutes = blockUntilTime.first * 60 + blockUntilTime.second
        
        // Block if current time is before the "block until" time
        return currentMinutes <= blockUntilMinutes
    }
    
    /**
     * Check if a SCHEDULED rule is active (recurring schedule)
     */
    private fun checkScheduledRule(rule: com.focusr.v2.models.BlockingRule): Boolean {
        val fromTime = rule.fromTime ?: return false
        val toTime = rule.toTime ?: return false
        
        val calendar = Calendar.getInstance()
        val currentDay = com.focusr.v2.models.DayOfWeek.getCurrentDay()
        
        // Check if today is in the rule's active days
        if (!rule.daysOfWeek.contains(currentDay)) {
            return false
        }
        
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val fromMinutes = fromTime.first * 60 + fromTime.second
        val toMinutes = toTime.first * 60 + toTime.second
        
        return if (fromMinutes > toMinutes) {
            // Overnight schedule (e.g., 10 PM to 2 AM)
            currentMinutes >= fromMinutes || currentMinutes <= toMinutes
        } else {
            // Same-day schedule (e.g., 9 AM to 5 PM)
            currentMinutes in fromMinutes..toMinutes
        }
    }
    
    /**
     * Get count of currently active rules
     */
    suspend fun getActiveRulesCount(): Int {
        val allRules = preferencesManager.getAllActiveRules()
        return allRules.count { isRuleActive(it) }
    }
    
    /**
     * Get all apps that should currently be blocked
     */
    suspend fun getCurrentlyBlockedApps(): Set<String> {
        val allRules = preferencesManager.blockingRules.first()
        return allRules
            .filter { it.enabled && isRuleActive(it) }
            .map { it.packageName }
            .toSet()
    }
}