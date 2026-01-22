package com.focusr.v2.models

import java.util.*

/**
 * Represents a blocking rule that can apply to one or multiple apps.
 * Supports alarm-style rule creation with precedence handling.
 */
data class BlockingRule(
    val id: String = UUID.randomUUID().toString(),
    
    // Rule name for alarm-style UI
    val name: String = "",
    
    // Multiple apps support
    val packageNames: List<String> = emptyList(),
    
    // OLD: Single app (kept for backward compatibility)
    @Deprecated("Use packageNames instead")
    val packageName: String = "",
    
    val ruleType: RuleType,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    
    // For SIMPLE rules - duration-based blocking
    val durationMinutes: Int? = null,  // Duration in minutes (15, 30, 60, 120, etc.)
    val activatedAt: Long? = null,     // Timestamp when rule was activated
    
    // OLD: block until time (kept for backward compatibility migration)
    @Deprecated("Use durationMinutes instead")
    val blockUntilTime: Pair<Int, Int>? = null,  // (hour, minute)
    
    // For SCHEDULED rules - recurring schedule
    val fromTime: Pair<Int, Int>? = null,  // (hour, minute)
    val toTime: Pair<Int, Int>? = null,    // (hour, minute)
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.values().toSet(),  // Default: all days
    
    // For MENTAL_CLARITY rules - smart sleep-aware blocking
    val windDownTime: Pair<Int, Int>? = null,    // Wind-down start time (hour, minute)
    val windDownEnabled: Boolean = true,          // Toggle wind-down phase
    val morningBlockDuration: Int? = null,        // Morning block duration in minutes
    val morningFuryEnabled: Boolean = true,       // Toggle morning blocking phase
    val sleepDetectionMinutes: Int = 300,         // No usage for X minutes = sleep (default 5 hours)
    val morningWindowStart: Int = 4,              // Morning detection window start hour (default 4 AM)
    val morningWindowEnd: Int = 12,               // Morning detection window end hour (default 12 PM)
    val wakeUpDetectedAt: Long? = null            // Timestamp when wake-up was detected
) {
    /**
     * Gets the actual package names, handling backward compatibility
     */
    fun getApps(): List<String> {
        return when {
            packageNames.isNotEmpty() -> packageNames
            packageName.isNotEmpty() -> listOf(packageName)
            else -> emptyList()
        }
    }
    
    /**
     * Gets the display name for the rule
     */
    fun getDisplayName(): String {
        return name.ifEmpty {
            val apps = getApps()
            when {
                apps.isEmpty() -> "Unnamed Rule"
                apps.size == 1 -> "Rule for ${apps.first()}"
                else -> "Rule for ${apps.size} apps"
            }
        }
    }
    
    /**
     * Gets the priority based on rule type (SIMPLE=1, SCHEDULED=2, MENTAL_CLARITY=3)
     */
    fun calculatePriority(): Int = when (ruleType) {
        RuleType.SIMPLE -> 1
        RuleType.SCHEDULED -> 2
        RuleType.MENTAL_CLARITY -> 3
    }
    
    /**
     * Validates that the rule has the required fields for its type
     */
    fun isValid(): Boolean {
        val hasApps = getApps().isNotEmpty()
        val hasValidTimes = when (ruleType) {
            RuleType.SIMPLE -> durationMinutes != null && durationMinutes > 0
            RuleType.SCHEDULED -> fromTime != null && toTime != null
            RuleType.MENTAL_CLARITY -> {
                // Must have either wind-down or morning fury enabled with valid settings
                val hasWindDown = windDownEnabled && windDownTime != null
                val hasMorningFury = morningFuryEnabled && morningBlockDuration != null && morningBlockDuration > 0
                hasWindDown || hasMorningFury
            }
        }
        return hasApps && hasValidTimes
    }
    
    /**
     * Returns a human-readable description of the rule
     */
    fun getDescription(): String {
        return when (ruleType) {
            RuleType.SIMPLE -> {
                val duration = durationMinutes ?: return "Invalid rule"
                val durationText = formatDuration(duration)
                if (activatedAt != null) {
                    val remaining = getRemainingMinutes()
                    if (remaining != null && remaining > 0) {
                        "$durationText (${formatDuration(remaining)} left)"
                    } else {
                        "$durationText (expired)"
                    }
                } else {
                    "For $durationText"
                }
            }
            RuleType.SCHEDULED -> {
                val (fromH, fromM) = fromTime ?: return "Invalid rule"
                val (toH, toM) = toTime ?: return "Invalid rule"
                val daysText = if (daysOfWeek.size == 7) "Daily" else daysOfWeek.joinToString(", ") { it.shortName }
                "${formatTime(fromH, fromM)} - ${formatTime(toH, toM)} ($daysText)"
            }
            RuleType.MENTAL_CLARITY -> {
                val parts = mutableListOf<String>()
                if (windDownEnabled && windDownTime != null) {
                    parts.add("Wind-down: ${formatTime(windDownTime.first, windDownTime.second)}")
                }
                if (morningFuryEnabled && morningBlockDuration != null) {
                    parts.add("Morning: ${formatDuration(morningBlockDuration)}")
                }
                if (parts.isEmpty()) "Not configured" else parts.joinToString(" • ")
            }
        }
    }
    
    /**
     * Gets remaining minutes for SIMPLE rules
     */
    fun getRemainingMinutes(): Int? {
        if (ruleType != RuleType.SIMPLE) return null
        val duration = durationMinutes ?: return null
        val activated = activatedAt ?: return null
        val expiresAt = activated + (duration * 60 * 1000L)
        val remaining = (expiresAt - System.currentTimeMillis()) / 1000 / 60
        return remaining.toInt().coerceAtLeast(0)
    }
    
    /**
     * Returns app count description
     */
    fun getAppCountDescription(): String {
        val count = getApps().size
        return when (count) {
            0 -> "No apps"
            1 -> "1 app"
            else -> "$count apps"
        }
    }
    
    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", displayHour, minute, period)
    }
    
    private fun formatDuration(minutes: Int): String {
        return when {
            minutes < 60 -> "${minutes}m"
            minutes % 60 == 0 -> "${minutes / 60}h"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }
}

/**
 * Types of blocking rules
 */
enum class RuleType {
    SIMPLE,         // Block for duration - Priority 1
    SCHEDULED,      // Recurring schedule with days of week - Priority 2
    MENTAL_CLARITY  // Smart sleep-aware blocking (wind-down + morning) - Priority 3
}

/**
 * Days of the week for scheduled rules
 */
enum class DayOfWeek(val shortName: String, val calendarValue: Int) {
    MONDAY("Mon", Calendar.MONDAY),
    TUESDAY("Tue", Calendar.TUESDAY),
    WEDNESDAY("Wed", Calendar.WEDNESDAY),
    THURSDAY("Thu", Calendar.THURSDAY),
    FRIDAY("Fri", Calendar.FRIDAY),
    SATURDAY("Sat", Calendar.SATURDAY),
    SUNDAY("Sun", Calendar.SUNDAY);
    
    companion object {
        /**
         * Converts Calendar day constant to DayOfWeek enum
         */
        fun fromCalendar(calendarDay: Int): DayOfWeek {
            return values().first { it.calendarValue == calendarDay }
        }
        
        /**
         * Gets the current day of week
         */
        fun getCurrentDay(): DayOfWeek {
            val calendar = Calendar.getInstance()
            return fromCalendar(calendar.get(Calendar.DAY_OF_WEEK))
        }
        
        /**
         * Returns weekdays (Monday-Friday)
         */
        fun weekdays(): Set<DayOfWeek> {
            return setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
        }
        
        /**
         * Returns weekend days (Saturday-Sunday)
         */
        fun weekend(): Set<DayOfWeek> {
            return setOf(SATURDAY, SUNDAY)
        }
    }
}
