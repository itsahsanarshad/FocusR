package com.focusr.v2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.first
import java.util.*

class ServiceScheduler(private val context: Context) {
    private val preferencesManager = PreferencesManager(context)
    private val blockingTimeManager = BlockingTimeManager(preferencesManager)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleService() {
        val advancedMode = preferencesManager.advancedMode.first()

        if (!advancedMode) {
            // Simple mode: always start service
            startMonitoringService()
            return
        }

        // Advanced mode: calculate when to start service using unified logic
        val startTime = blockingTimeManager.calculateNextStartTime()
        if (startTime <= System.currentTimeMillis()) {
            // Should start now
            startMonitoringService()
        } else {
            // Schedule for later
            scheduleServiceStart(startTime)
        }
    }

    private fun scheduleServiceStart(startTime: Long) {
        val intent = Intent(context, ServiceStartReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel any existing alarms
        alarmManager.cancel(pendingIntent)

        // Schedule new alarm
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            startTime,
            pendingIntent
        )

        Log.d("ServiceScheduler", "Service scheduled to start at: ${Date(startTime)}")
    }

    private fun startMonitoringService() {
        val intent = Intent(context, AppMonitoringService::class.java)
        context.startForegroundService(intent)
    }

    fun cancelScheduledService() {
        val intent = Intent(context, ServiceStartReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun resetService() {
        // Cancel any scheduled service start
        cancelScheduledService()

        // Stop the monitoring service
        val intent = Intent(context, AppMonitoringService::class.java)
        context.stopService(intent)

        Log.d("ServiceScheduler", "Service reset - stopped service and cancelled schedules")
    }

}