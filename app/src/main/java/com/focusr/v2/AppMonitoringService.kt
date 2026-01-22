package com.focusr.v2

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat

class AppMonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var monitoringRunnable: Runnable? = null
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var blockingTimeManager: BlockingTimeManager
    private lateinit var sleepDetectionManager: SleepDetectionManager
    private val notificationHandler = Handler(Looper.getMainLooper())
    private var notificationRunnable: Runnable? = null
    private var lastDetectedApp: String? = null

    companion object {
        const val CHANNEL_ID = "focus_blocker_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        blockingTimeManager = BlockingTimeManager(preferencesManager)
        sleepDetectionManager = SleepDetectionManager(this)
        createNotificationChannel()
        
        // Perform migration on service creation
        serviceScope.launch {
            preferencesManager.migrateOldBlockedApps()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
            startNotificationUpdates()
        }
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        monitoringRunnable = object : Runnable {
            override fun run() {
                serviceScope.launch {
                    checkCurrentApp()
                }
                handler.postDelayed(this, 3000) // Check every 3 seconds
            }
        }
        handler.post(monitoringRunnable!!)
    }

    private suspend fun checkCurrentApp() {
        try {
            // Get current foreground app
            val currentApp = getCurrentForegroundApp()
            lastDetectedApp = currentApp
            Log.d("AppMonitoringService", "Foreground app detected: $currentApp")
            
            if (currentApp == null) return
            
            delay(100)
            
            // NEW: Check if THIS specific app should be blocked based on its rules
            if (blockingTimeManager.shouldBlockApp(currentApp)) {
                Log.d("AppMonitoringService", "Blocking $currentApp based on active rules")
                blockApp(currentApp)
            }
            
            // Auto-disable expired SIMPLE rules
            val allRules = preferencesManager.blockingRules.first()
            val expiredSimpleRules = allRules.filter { rule ->
                rule.enabled && 
                rule.ruleType == com.focusr.v2.models.RuleType.SIMPLE &&
                rule.durationMinutes != null &&
                rule.activatedAt != null &&
                System.currentTimeMillis() >= rule.activatedAt + (rule.durationMinutes * 60 * 1000L)
            }
            
            // Disable expired rules
            expiredSimpleRules.forEach { expiredRule ->
                Log.d("AppMonitoringService", "Auto-disabling expired SIMPLE rule: ${expiredRule.getDisplayName()}")
                preferencesManager.updateRule(expiredRule.copy(enabled = false))
            }
            
            // Check for wake-up detection for Mental Clarity rules
            val mentalClarityRules = allRules.filter { rule ->
                rule.enabled &&
                rule.ruleType == com.focusr.v2.models.RuleType.MENTAL_CLARITY &&
                rule.morningFuryEnabled &&
                rule.wakeUpDetectedAt == null  // Not yet detected
            }
            
            mentalClarityRules.forEach { rule ->
                // Check if we're in the morning window
                if (sleepDetectionManager.isInMorningWindow(rule.morningWindowStart, rule.morningWindowEnd)) {
                    // Try to detect wake-up
                    val wakeUpTime = sleepDetectionManager.detectWakeUp(
                        rule.sleepDetectionMinutes,
                        rule.morningWindowStart,
                        rule.morningWindowEnd
                    )
                    if (wakeUpTime != null) {
                        Log.d("AppMonitoringService", "Wake-up detected for Mental Clarity rule: ${rule.getDisplayName()}")
                        preferencesManager.updateRule(rule.copy(wakeUpDetectedAt = wakeUpTime))
                    }
                }
            }
            
            // Auto-clear expired Mental Clarity morning blocks and reset for next day
            val expiredMentalClarityRules = allRules.filter { rule ->
                rule.enabled &&
                rule.ruleType == com.focusr.v2.models.RuleType.MENTAL_CLARITY &&
                rule.wakeUpDetectedAt != null &&
                rule.morningBlockDuration != null &&
                System.currentTimeMillis() >= rule.wakeUpDetectedAt + (rule.morningBlockDuration * 60 * 1000L)
            }
            
            expiredMentalClarityRules.forEach { rule ->
                Log.d("AppMonitoringService", "Resetting Mental Clarity rule for next day: ${rule.getDisplayName()}")
                // Reset wakeUpDetectedAt so it can detect again tomorrow
                preferencesManager.updateRule(rule.copy(wakeUpDetectedAt = null))
            }
            
            // Check if any enabled rules remain
            val enabledRules = preferencesManager.blockingRules.first().filter { it.enabled }

            if (enabledRules.isEmpty()) {
                Log.d("AppMonitoringService", "No enabled rules exist, stopping service")
                stopSelf()
                return
            }

            // If we have enabled rules but none are active, keep running
            // (they might become active soon, e.g., SCHEDULED rules)
            val activeRulesCount = blockingTimeManager.getActiveRulesCount()
            Log.d("AppMonitoringService", "Enabled rules: ${enabledRules.size}, Active rules: $activeRulesCount")

        } catch (e: Exception) {
            Log.e("AppMonitoringService", "Error in checkCurrentApp: ${e.message}")
        }
    }

    private fun getCurrentForegroundApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()

        // Get usage stats for the last 10 seconds
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 10000,
            time
        )

        Log.d("ForegroundApp", "Stats list size: ${usageStatsList?.size}")

        if (usageStatsList.isNullOrEmpty()) {
            Log.d("ForegroundApp", "No usage stats available")
            return null
            }
                // Log all recent apps
    //usageStatsList.forEach {
     //   Log.d("ForegroundApp", "App: ${it.packageName}, Last used: ${time - it.lastTimeUsed}ms ago")
   // }

        // Find the most recently used app
        val recentApp = usageStatsList.maxByOrNull { it.lastTimeUsed }
        Log.d("ForegroundApp", "Selected: ${recentApp?.packageName}")
        return recentApp?.packageName
    }

    private fun blockApp(packageName: String) {
        val intent = Intent(this, BlockerOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("blocked_app", packageName)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus Blocker Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification channel for Focus Blocker foreground service"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private suspend fun buildNotification(): Notification {
        // NEW: Show count of active rules
        val activeRulesCount = blockingTimeManager.getActiveRulesCount()
        val blockedAppsCount = blockingTimeManager.getCurrentlyBlockedApps().size
        
        val message = when {
            activeRulesCount == 0 -> "No active rules"
            blockedAppsCount == 1 -> "Blocking 1 app"
            blockedAppsCount > 1 -> "Blocking $blockedAppsCount apps"
            else -> "$activeRulesCount rules active"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Blocker")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun startNotificationUpdates() {
        notificationRunnable = object : Runnable {
            override fun run() {
                serviceScope.launch {
                    val updatedNotification = buildNotification()
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification)
                }
                notificationHandler.postDelayed(this, 10000) // Update every 10sec
            }
        }
        notificationHandler.post(notificationRunnable!!)
    }

    private fun stopNotificationUpdates() {
        notificationRunnable?.let { notificationHandler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()

        stopNotificationUpdates()
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        serviceScope.cancel()
    }
}