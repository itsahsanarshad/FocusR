//package com.focusr.v2
//
//import android.app.AlarmManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import kotlinx.coroutines.flow.first
//
//class ServiceManager(private val context: Context) {
//    private val preferencesManager = PreferencesManager(context)
//    private val blockingTimeManager = BlockingTimeManager(preferencesManager)
//    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//    suspend fun toggleBlocking(enabled: Boolean) {
//        if (enabled) {
//            val advancedMode = preferencesManager.advancedMode.first()
//            val nextStart = blockingTimeManager.calculateNextStartTime()
//
//            if (advancedMode) {
//                if (nextStart <= System.currentTimeMillis()) {
//                    startBlocking()
//                } else {
//                    scheduleNextBlocking(nextStart)
//                }
//            } else { //Simple mode
//
//                    startBlocking()
//            }
//        } else {
//            stopBlocking()
//        }
//    }
//
//    private fun startBlocking() {
//        val intent = Intent(context, AppMonitoringService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
//        else context.startService(intent)
//        blockingTimeManager.resetFlags()
//    }
//
//    private fun stopBlocking() {
//        val intent = Intent(context, AppMonitoringService::class.java)
//        context.stopService(intent)
//        cancelScheduledBlocking()
//        blockingTimeManager.resetFlags()
//    }
//
//    private fun scheduleNextBlocking(timeMillis: Long) {
//        val intent = Intent(context, ServiceStartReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(
//            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        alarmManager.cancel(pendingIntent)
//        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
//    }
//
//    private fun cancelScheduledBlocking() {
//        val intent = Intent(context, ServiceStartReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(
//            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        alarmManager.cancel(pendingIntent)
//    }
//}



//package com.focusr.v2
//
//import android.app.AlarmManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import com.focusr.v2.data.TimerFeature
//import kotlinx.coroutines.flow.first
//
//class ServiceManager(private val context: Context) {
//    private val preferencesManager = PreferencesManager(context)
//    private val blockingTimeManager = BlockingTimeManager(preferencesManager)
//    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//
////    suspend fun toggleBlocking(enabled: Boolean) {
////        if (enabled) {
////            val timerSettings = preferencesManager.timerSettings.first()
////            val effectiveAdvancedMode = timerSettings.enabledFeatures.contains(TimerFeature.ADVANCED_MODE) ||
////                    (!timerSettings.enabledFeatures.contains(TimerFeature.SIMPLE_MODE) &&
////                            preferencesManager.advancedMode.first())
////            val nextStart = blockingTimeManager.calculateNextStartTime()
////
////            if (effectiveAdvancedMode) {
////                if (nextStart <= System.currentTimeMillis()) {
////                    startBlocking()
////                } else {
////                    scheduleNextBlocking(nextStart)
////                }
////            } else { //Simple mode
////
////                startBlocking()
////            }
////        } else {
////            stopBlocking()
////        }
////    }
//
//    // Update the toggleBlocking function in ServiceManager.kt
//
//    suspend fun toggleBlocking(enabled: Boolean) {
//        if (enabled) {
//            val timerSettings = preferencesManager.timerSettings.first()
//            val effectiveAdvancedMode = timerSettings.enabledFeatures.contains(TimerFeature.ADVANCED_MODE) ||
//                    (!timerSettings.enabledFeatures.contains(TimerFeature.SIMPLE_MODE) &&
//                            preferencesManager.advancedMode.first())
//
//            if (effectiveAdvancedMode) {
//                // Advanced mode: Use existing time-based logic
//                val nextStart = blockingTimeManager.calculateNextStartTime()
//                if (nextStart <= System.currentTimeMillis()) {
//                    startBlocking()
//                } else {
//                    scheduleNextBlocking(nextStart)
//                }
//            } else {
//                // Simple mode: Start timer immediately
//                blockingTimeManager.startSimpleModeSession()
//                startBlocking()
//            }
//        } else {
//            stopBlocking()
//            // Stop simple mode session if it was running
//            val timerSettings = preferencesManager.timerSettings.first()
//            val effectiveAdvancedMode = timerSettings.enabledFeatures.contains(TimerFeature.ADVANCED_MODE) ||
//                    (!timerSettings.enabledFeatures.contains(TimerFeature.SIMPLE_MODE) &&
//                            preferencesManager.advancedMode.first())
//
//            if (!effectiveAdvancedMode) {
//                blockingTimeManager.stopSimpleModeSession()
//            }
//        }
//    }
//
//    private fun startBlocking() {
//        val intent = Intent(context, AppMonitoringService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
//        else context.startService(intent)
//        blockingTimeManager.resetFlags()
//    }
//
//    private fun stopBlocking() {
//        val intent = Intent(context, AppMonitoringService::class.java)
//        context.stopService(intent)
//        cancelScheduledBlocking()
//        blockingTimeManager.resetFlags()
//    }
//
//    private fun scheduleNextBlocking(timeMillis: Long) {
//        val intent = Intent(context, ServiceStartReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(
//            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        alarmManager.cancel(pendingIntent)
//        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
//    }
//
//    private fun cancelScheduledBlocking() {
//        val intent = Intent(context, ServiceStartReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(
//            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        alarmManager.cancel(pendingIntent)
//    }
//}


package com.focusr.v2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.focusr.v2.data.TimerFeature
import kotlinx.coroutines.flow.first

class ServiceManager(private val context: Context) {
    private val preferencesManager = PreferencesManager(context)
    private val blockingTimeManager = BlockingTimeManager(preferencesManager)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "ServiceManager"
    }

    suspend fun toggleBlocking(enabled: Boolean) {
        Log.d(TAG, "toggleBlocking called with enabled: $enabled")

        if (enabled) {
            val activeMode = determineActiveMode()
            Log.d(TAG, "Active mode determined: $activeMode")

            when (activeMode) {
                TimerFeature.SIMPLE_MODE -> {
                    Log.d(TAG, "Starting Simple Mode")
                    handleSimpleMode()
                }
                TimerFeature.ADVANCED_MODE -> {
                    Log.d(TAG, "Starting Advanced Mode")
                    handleAdvancedMode()
                }
                TimerFeature.DAILY_USAGE_LIMIT -> {
                    Log.d(TAG, "Starting Daily Usage Limit Mode")
                    handleDailyUsageMode()
                }
                TimerFeature.BREAK_REMINDERS -> {
                    Log.d(TAG, "Starting Usage Reminder Mode")
                    handleUsageReminderMode()
                }
                else -> {
                    Log.w(TAG, "No active mode found, defaulting to Simple Mode")
                    handleSimpleMode()
                }
            }
        } else {
            Log.d(TAG, "Stopping all blocking services")
            stopAllServices()
        }
    }

    private suspend fun determineActiveMode(): TimerFeature? {
        val timerSettings = preferencesManager.timerSettings.first()
        val enabledFeatures = timerSettings.enabledFeatures

        Log.d(TAG, "Enabled features: $enabledFeatures")

        // Priority order: Simple -> Advanced -> Daily Limit -> Reminder
        return when {
            enabledFeatures.contains(TimerFeature.SIMPLE_MODE) -> {
                Log.d(TAG, "Simple Mode is enabled")
                TimerFeature.SIMPLE_MODE
            }
            enabledFeatures.contains(TimerFeature.ADVANCED_MODE) -> {
                Log.d(TAG, "Advanced Mode is enabled")
                TimerFeature.ADVANCED_MODE
            }
            enabledFeatures.contains(TimerFeature.DAILY_USAGE_LIMIT) -> {
                Log.d(TAG, "Daily Usage Limit is enabled")
                TimerFeature.DAILY_USAGE_LIMIT
            }
            enabledFeatures.contains(TimerFeature.BREAK_REMINDERS) -> {
                Log.d(TAG, "Usage Reminder is enabled")
                TimerFeature.BREAK_REMINDERS
            }
            else -> {
                // Fallback to old advanced mode setting
                val oldAdvancedMode = preferencesManager.advancedMode.first()
                Log.d(TAG, "No timer features enabled, old advanced mode: $oldAdvancedMode")
                if (oldAdvancedMode) TimerFeature.ADVANCED_MODE else TimerFeature.SIMPLE_MODE
            }
        }
    }

    private suspend fun handleSimpleMode() {
        Log.d(TAG, "Handling Simple Mode")
        blockingTimeManager.startSimpleModeSession()
        startBlockingService()
    }

    private suspend fun handleAdvancedMode() {
        Log.d(TAG, "Handling Advanced Mode")
        val nextStart = blockingTimeManager.calculateNextStartTime()
        Log.d(TAG, "Next start time calculated: $nextStart, current time: ${System.currentTimeMillis()}")

        if (nextStart <= System.currentTimeMillis()) {
            Log.d(TAG, "Starting blocking immediately")
            startBlockingService()
        } else {
            Log.d(TAG, "Scheduling next blocking for: $nextStart")
            scheduleNextBlocking(nextStart)
        }
    }

    private suspend fun handleDailyUsageMode() {
        Log.d(TAG, "Handling Daily Usage Mode")
        // Check if daily limit is already reached
        val timerSettings = preferencesManager.timerSettings.first()
        val dailyUsage = preferencesManager.dailyUsageMinutes.first()

        Log.d(TAG, "Daily usage: ${dailyUsage}min, limit: ${timerSettings.dailyLimitMinutes}min")

        if (dailyUsage >= timerSettings.dailyLimitMinutes) {
            Log.d(TAG, "Daily limit already reached, starting blocking immediately")
            startBlockingService()
        } else {
            Log.d(TAG, "Daily limit not reached, starting usage monitoring service")
            startUsageMonitoringService()
        }
    }

    private suspend fun handleUsageReminderMode() {
        Log.d(TAG, "Handling Usage Reminder Mode")
        startUsageReminderService()
    }

    private fun startBlockingService() {
        Log.d(TAG, "Starting AppMonitoringService (Blocking)")
        val intent = Intent(context, AppMonitoringService::class.java).apply {
            putExtra("service_mode", "blocking")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        blockingTimeManager.resetFlags()
    }

    private fun startUsageMonitoringService() {
        Log.d(TAG, "Starting AppMonitoringService (Usage Monitoring)")
        val intent = Intent(context, AppMonitoringService::class.java).apply {
            putExtra("service_mode", "usage_monitoring")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun startUsageReminderService() {
        Log.d(TAG, "Starting AppMonitoringService (Usage Reminder)")
        val intent = Intent(context, AppMonitoringService::class.java).apply {
            putExtra("service_mode", "usage_reminder")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private suspend fun stopAllServices() {
        Log.d(TAG, "Stopping all services")

        // Stop the main monitoring service
        val intent = Intent(context, AppMonitoringService::class.java)
        context.stopService(intent)

        // Cancel any scheduled alarms
        cancelScheduledBlocking()

        // Reset all session data
        blockingTimeManager.resetFlags()
        blockingTimeManager.stopSimpleModeSession()
        blockingTimeManager.stopUsageReminderSession()

        Log.d(TAG, "All services stopped and sessions reset")
    }

    private fun scheduleNextBlocking(timeMillis: Long) {
        Log.d(TAG, "Scheduling next blocking for: $timeMillis")
        val intent = Intent(context, ServiceStartReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        Log.d(TAG, "Alarm scheduled successfully")
    }

    private fun cancelScheduledBlocking() {
        Log.d(TAG, "Canceling scheduled blocking")
        val intent = Intent(context, ServiceStartReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Scheduled blocking canceled")
    }
}
