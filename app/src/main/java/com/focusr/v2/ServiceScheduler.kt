package com.focusr.v2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.focusr.v2.models.BlockingRule
import com.focusr.v2.models.RuleType
import kotlinx.coroutines.flow.first
import java.util.*


class ServiceScheduler(private val context: Context) {
    private val preferencesManager = PreferencesManager(context)
    private val blockingTimeManager = BlockingTimeManager(preferencesManager)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    companion object {
        private const val REQUEST_CODE = 1001
        private const val PRE_START_MINUTES = 5 // Start 5 min before scheduled time
    }

    /**
     * NEW: Schedule or start service based on rules (rule-based system)
     */
suspend fun scheduleService() {
    val allRules = preferencesManager.blockingRules.first()
    val enabledRules = allRules.filter { it.enabled }
    
    Log.d("ServiceScheduler", "=== scheduleService() called ===")
    Log.d("ServiceScheduler", "Total rules: ${allRules.size}, Enabled: ${enabledRules.size}")
    
    if (enabledRules.isEmpty()) {
        Log.d("ServiceScheduler", "No enabled rules, canceling service")
        cancelScheduledService()
        stopService()
        return
    }
    
    // Check if any rules need immediate service start
    val hasSimpleRules = enabledRules.any { it.ruleType == RuleType.SIMPLE }
    val hasSmartCooldownRules = enabledRules.any { it.ruleType == RuleType.SMART_COOLDOWN }
    val hasMentalClarityRules = enabledRules.any { it.ruleType == RuleType.MENTAL_CLARITY }
    val hasActiveRules = enabledRules.any { blockingTimeManager.isRuleActive(it) }
    
    Log.d("ServiceScheduler", "Has SIMPLE: $hasSimpleRules, SMART_COOLDOWN: $hasSmartCooldownRules, MENTAL_CLARITY: $hasMentalClarityRules, Active: $hasActiveRules")
    
    // Start service immediately for any of these conditions:
    // - SIMPLE rules (always need monitoring)
    // - SMART_COOLDOWN rules (need session tracking)
    // - MENTAL_CLARITY rules (need sleep detection)
    // - Any currently active rule
    if (hasSimpleRules || hasSmartCooldownRules || hasMentalClarityRules || hasActiveRules) {
        Log.d("ServiceScheduler", "Starting service immediately")
        startMonitoringService()
        cancelScheduledService()
    } else {
        // All rules are SCHEDULED and not active yet
        Log.d("ServiceScheduler", "All rules are SCHEDULED and not active")
        
        // Find next activation time
        val nextStartTime = calculateNextStartTime(enabledRules)
        
        if (nextStartTime != null) {
            Log.d("ServiceScheduler", "Next start time calculated: ${java.util.Date(nextStartTime)}")
            scheduleServiceStart(nextStartTime)
        } else {
            Log.d("ServiceScheduler", "No upcoming scheduled times")
            stopService()
        }
    }
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
                // FIXED: Check if rule is currently active first
                val isCurrentlyActive = blockingTimeManager.isRuleActive(rule)
                
                if (isCurrentlyActive) {
                    // Rule is active NOW - don't schedule, service should already be running
                    Log.d("ServiceScheduler", "Rule '${rule.getDisplayName()}' is currently active - no scheduling needed")
                    return@mapNotNull null
                }
                
                val startCal = Calendar.getInstance()
                startCal.set(Calendar.HOUR_OF_DAY, hour)
                startCal.set(Calendar.MINUTE, minute)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                
                // Subtract 5 minutes for pre-start
                startCal.add(Calendar.MINUTE, -PRE_START_MINUTES)
                
                var startTime = startCal.timeInMillis
                
                Log.d("ServiceScheduler", "Rule: ${rule.getDisplayName()}, Original time: $hour:$minute, Pre-start time: ${java.util.Date(startTime)}")
                
                // If time has passed today, schedule for next occurrence
                if (startTime <= now) {
                    startCal.add(Calendar.DAY_OF_YEAR, 1)
                    startTime = startCal.timeInMillis
                    Log.d("ServiceScheduler", "Time passed, rescheduled for tomorrow: ${java.util.Date(startTime)}")
                }
                
                startTime
            }
        }
    
    val nextTime = scheduledTimes.minOrNull()
    Log.d("ServiceScheduler", "Next start time: ${if (nextTime != null) java.util.Date(nextTime) else "null"}")
    
    return nextTime
}

private fun scheduleServiceStart(startTime: Long) {
    val intent = Intent(context, ServiceStartReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    // Cancel any existing alarms
    alarmManager.cancel(pendingIntent)
    // Check if we can schedule exact alarms (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.e("ServiceScheduler", "Cannot schedule exact alarms - permission not granted!")
            // Fallback: start service now instead of scheduling
            startMonitoringService()
            return
        }
    }
    // Schedule new alarm
    try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            startTime,
            pendingIntent
        )
        
        val timeUntil = (startTime - System.currentTimeMillis()) / 1000 / 60
        Log.d("ServiceScheduler", "✓ Service scheduled to start in $timeUntil minutes at ${java.util.Date(startTime)}")
    } catch (e: SecurityException) {
        Log.e("ServiceScheduler", "SecurityException scheduling alarm: ${e.message}")
        // Fallback: start service now
        startMonitoringService()
    }
}

    private fun startMonitoringService() {
        val intent = Intent(context, AppMonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
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
        Log.d("ServiceScheduler", "Canceled scheduled service")
    }
    
    private fun stopService() {
        val intent = Intent(context, AppMonitoringService::class.java)
        context.stopService(intent)
    }

    fun resetService() {
        // Cancel any scheduled service start
        cancelScheduledService()

        // Stop the monitoring service
        stopService()

        Log.d("ServiceScheduler", "Service reset - stopped service and cancelled schedules")
    }
}