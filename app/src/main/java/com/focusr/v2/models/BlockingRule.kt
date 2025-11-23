package com.focusr.v2.models

import java.util.*

/**
 * Represents a blocking rule that can apply to one or multiple apps.
 * Supports alarm-style rule creation with precedence handling.
 */
data class BlockingRule(
    val id: String = UUID.randomUUID().toString(),
    
    // NEW: Rule name for alarm-style UI
    val name: String = "",
    
    // NEW: Multiple apps support
    val packageNames: List<String> = emptyList(),
    
    // OLD: Single app (kept for backward compatibility)
    @Deprecated("Use packageNames instead")
    val packageName: String = "",
    
    val ruleType: RuleType,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    
    // For SIMPLE rules - block until specific time today
    val blockUntilTime: Pair<Int, Int>? = null,  // (hour, minute)
    
    // For SCHEDULED rules - recurring schedule
    val fromTime: Pair<Int, Int>? = null,  // (hour, minute)
    val toTime: Pair<Int, Int>? = null,    // (hour, minute)
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.values().toSet()  // Default: all days
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
     * Gets the priority based on rule type (SIMPLE=1, SCHEDULED=2)
     */
    fun calculatePriority(): Int = when (ruleType) {
        RuleType.SIMPLE -> 1
        RuleType.SCHEDULED -> 2
    }
    
    /**
     * Validates that the rule has the required fields for its type
     */
    fun isValid(): Boolean {
        val hasApps = getApps().isNotEmpty()
        val hasValidTimes = when (ruleType) {
            RuleType.SIMPLE -> blockUntilTime != null
            RuleType.SCHEDULED -> fromTime != null && toTime != null
        }
        return hasApps && hasValidTimes
    }
    
    /**
     * Returns a human-readable description of the rule
     */
    fun getDescription(): String {
        return when (ruleType) {
            RuleType.SIMPLE -> {
                val (hour, minute) = blockUntilTime ?: return "Invalid rule"
                "Until ${formatTime(hour, minute)} today"
            }
            RuleType.SCHEDULED -> {
                val (fromH, fromM) = fromTime ?: return "Invalid rule"
                val (toH, toM) = toTime ?: return "Invalid rule"
                val daysText = if (daysOfWeek.size == 7) "Daily" else daysOfWeek.joinToString(", ") { it.shortName }
                "${formatTime(fromH, fromM)} - ${formatTime(toH, toM)} ($daysText)"
            }
        }
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
}

/**
 * Types of blocking rules
 */
enum class RuleType {
    SIMPLE,     // Block until specific time (same day) - Priority 1
    SCHEDULED   // Recurring schedule with days of week - Priority 2
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
