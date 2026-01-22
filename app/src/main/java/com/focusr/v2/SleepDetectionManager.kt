package com.focusr.v2

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import java.util.*

/**
 * Manages sleep detection using usage gap analysis.
 * Analyzes app usage patterns to detect when the user was sleeping
 * and when they woke up.
 */
class SleepDetectionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SleepDetectionManager"
        private const val QUERY_WINDOW_HOURS = 12L  // Look back 12 hours for sleep detection
    }
    
    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }
    
    /**
     * Detects if the user has woken up from sleep based on usage gaps.
     * 
     * @param sleepThresholdMinutes Minimum gap (in minutes) to consider as sleep
     * @param morningWindowStart Start hour of morning detection window (e.g., 4 for 4 AM)
     * @param morningWindowEnd End hour of morning detection window (e.g., 12 for 12 PM)
     * @return Wake-up timestamp if detected within the morning window, null otherwise
     */
    fun detectWakeUp(
        sleepThresholdMinutes: Int,
        morningWindowStart: Int,
        morningWindowEnd: Int
    ): Long? {
        try {
            val now = System.currentTimeMillis()
            val queryStart = now - (QUERY_WINDOW_HOURS * 60 * 60 * 1000)
            
            // Get all usage events in the window
            val usageEvents = usageStatsManager.queryEvents(queryStart, now)
            val eventList = mutableListOf<UsageEvent>()
            
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                // Track foreground activity events (user actively using phone)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                    eventList.add(UsageEvent(event.timeStamp, event.eventType))
                }
            }
            
            if (eventList.size < 2) {
                Log.d(TAG, "Not enough usage events to detect sleep")
                return null
            }
            
            // Sort by timestamp
            eventList.sortBy { it.timestamp }
            
            // Find the longest gap between events
            val sleepThresholdMs = sleepThresholdMinutes * 60 * 1000L
            var longestGapStart = 0L
            var longestGapEnd = 0L
            var longestGapDuration = 0L
            
            for (i in 1 until eventList.size) {
                val gap = eventList[i].timestamp - eventList[i - 1].timestamp
                if (gap > longestGapDuration) {
                    longestGapDuration = gap
                    longestGapStart = eventList[i - 1].timestamp
                    longestGapEnd = eventList[i].timestamp
                }
            }
            
            Log.d(TAG, "Longest usage gap: ${longestGapDuration / 1000 / 60} minutes")
            
            // Check if the gap qualifies as sleep
            if (longestGapDuration >= sleepThresholdMs) {
                // Check if wake-up time is in morning window
                val wakeUpCalendar = Calendar.getInstance().apply { timeInMillis = longestGapEnd }
                val wakeUpHour = wakeUpCalendar.get(Calendar.HOUR_OF_DAY)
                
                if (wakeUpHour in morningWindowStart..morningWindowEnd) {
                    Log.d(TAG, "Wake-up detected at hour $wakeUpHour (within $morningWindowStart-$morningWindowEnd window)")
                    return longestGapEnd
                } else {
                    Log.d(TAG, "Gap detected but wake-up hour $wakeUpHour is outside morning window")
                }
            }
            
            return null
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Usage stats permission not granted: ${e.message}")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting wake-up: ${e.message}")
            return null
        }
    }
    
    /**
     * Gets the timestamp of the most recent app usage.
     */
    fun getLastUsageTime(): Long? {
        try {
            val now = System.currentTimeMillis()
            val queryStart = now - (60 * 60 * 1000) // Last hour
            
            val usageEvents = usageStatsManager.queryEvents(queryStart, now)
            var lastTimestamp: Long? = null
            
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastTimestamp = event.timeStamp
                }
            }
            
            return lastTimestamp
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last usage time: ${e.message}")
            return null
        }
    }
    
    /**
     * Checks if we're currently in the morning detection window.
     */
    fun isInMorningWindow(morningWindowStart: Int, morningWindowEnd: Int): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        return currentHour in morningWindowStart..morningWindowEnd
    }
    
    /**
     * Checks if we're currently after the wind-down time.
     */
    fun isAfterWindDownTime(windDownHour: Int, windDownMinute: Int): Boolean {
        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val windDownMinutes = windDownHour * 60 + windDownMinute
        return currentMinutes >= windDownMinutes
    }
    
    private data class UsageEvent(
        val timestamp: Long,
        val eventType: Int
    )
}
