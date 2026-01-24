package com.focusr.v2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.focusr.v2.models.BlockingRule
import com.focusr.v2.models.RuleType
import kotlinx.coroutines.flow.first
import java.util.*

/**
 * ServiceScheduler for FocusR.
 * 
 * Note: With Accessibility Service, we no longer need to start/stop a foreground service.
 * The Accessibility Service is managed by Android and runs when the user enables it
 * in Settings > Accessibility > FocusR.
 * 
 * This scheduler now only handles AlarmManager scheduling for edge cases like
 * scheduled rules that need to trigger at specific times.
 */
class ServiceScheduler(private val context: Context) {
    private val preferencesManager = PreferencesManager(context)
    private val blockingTimeManager = BlockingTimeManager(preferencesManager)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    companion object {
        private const val REQUEST_CODE = 1001
        private const val PRE_START_MINUTES = 5
    }

    /**
     * Called when rules change. With Accessibility Service, this is mostly a no-op
     * since the service is always running when enabled by the user.
     */
    suspend fun scheduleService() {
        val allRules = preferencesManager.blockingRules.first()
        val enabledRules = allRules.filter { it.enabled }
        
        Log.d("ServiceScheduler", "=== scheduleService() called ===")
        Log.d("ServiceScheduler", "Total rules: ${allRules.size}, Enabled: ${enabledRules.size}")
        
        if (enabledRules.isEmpty()) {
            Log.d("ServiceScheduler", "No enabled rules")
            cancelScheduledService()
            return
        }
        
        // With Accessibility Service, blocking happens automatically when apps are opened.
        // The service is managed by Android, not us.
        Log.d("ServiceScheduler", "Accessibility Service handles blocking - no action needed")
    }
    
    /**
     * Calculate when service should next start (5 min before SCHEDULED rules)
     */
    private fun calculateNextStartTime(rules: List<BlockingRule>): Long? {
        val now = System.currentTimeMillis()
        val currentCal = Calendar.getInstance()
        
        Log.d("ServiceScheduler", "Calculating next start time. Current time: ${java.util.Date(now)}")
        
        val scheduledTimes = rules
            .filter { it.ruleType == RuleType.SCHEDULED }
            .mapNotNull { rule ->
                rule.fromTime?.let { (hour, minute) ->
                    val isCurrentlyActive = blockingTimeManager.isRuleActive(rule)
                    
                    if (isCurrentlyActive) {
                        Log.d("ServiceScheduler", "Rule '${rule.getDisplayName()}' is currently active - no scheduling needed")
                        return@mapNotNull null
                    }
                    
                    val startCal = Calendar.getInstance()
                    startCal.set(Calendar.HOUR_OF_DAY, hour)
                    startCal.set(Calendar.MINUTE, minute)
                    startCal.set(Calendar.SECOND, 0)
                    startCal.set(Calendar.MILLISECOND, 0)

                    // Check if rule applies to current day
                    val currentDayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK)
                    val ruleAppliesOnThisDay = rule.daysOfWeek.isEmpty() || rule.daysOfWeek.any { it.calendarValue == currentDayOfWeek }
                    
                    // If start time has passed today
                    if (startCal.timeInMillis <= now) {
                        if (ruleAppliesOnThisDay) {
                            // Already past for today, look for next occurrence
                            startCal.add(Calendar.DAY_OF_YEAR, 1)
                        } else {
                            // Find the next day this rule applies
                            for (i in 1..7) {
                                startCal.add(Calendar.DAY_OF_YEAR, 1)
                                val dayOfWeek = startCal.get(Calendar.DAY_OF_WEEK)
                                if (rule.daysOfWeek.isEmpty() || rule.daysOfWeek.any { it.calendarValue == dayOfWeek }) {
                                    break
                                }
                            }
                        }
                    }

                    // Subtract pre-start buffer
                    val scheduleTime = startCal.timeInMillis - (PRE_START_MINUTES * 60 * 1000L)
                    
                    Log.d("ServiceScheduler", "Rule '${rule.getDisplayName()}' next start: ${java.util.Date(scheduleTime)}")
                    scheduleTime
                }
            }
            .filter { it > now }

        return scheduledTimes.minOrNull()
    }

    private fun scheduleServiceStart(startTime: Long) {
        val intent = Intent(context, ServiceStartReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                startTime,
                pendingIntent
            )
            
            val timeUntil = (startTime - System.currentTimeMillis()) / 1000 / 60
            Log.d("ServiceScheduler", "✓ Alarm scheduled for $timeUntil minutes at ${java.util.Date(startTime)}")
        } catch (e: SecurityException) {
            Log.e("ServiceScheduler", "SecurityException scheduling alarm: ${e.message}")
        }
    }

    fun cancelScheduledService() {
        val intent = Intent(context, ServiceStartReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("ServiceScheduler", "Canceled scheduled alarms")
    }

    fun resetService() {
        // Cancel any scheduled alarms
        cancelScheduledService()
        Log.d("ServiceScheduler", "Service reset - cancelled scheduled alarms")
    }
}