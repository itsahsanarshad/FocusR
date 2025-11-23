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
            
            // NEW: Stop service if no rules are currently active
          //  val activeRulesCount = blockingTimeManager.getActiveRulesCount()
       //     if (activeRulesCount == 0) {
      //          Log.d("AppMonitoringService", "No active rules, stopping service")
     //           stopSelf()
     //           return
     //       }

     // FIXED: Only stop if NO rules exist at all (not just if none are active)
val allRules = preferencesManager.blockingRules.first()
val enabledRules = allRules.filter { it.enabled }

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

        if (usageStatsList.isNullOrEmpty()) return null

        // Find the most recently used app
        val recentApp = usageStatsList.maxByOrNull { it.lastTimeUsed }
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
        // Reset timing flags when service stops
        blockingTimeManager.resetFlags()

        stopNotificationUpdates()
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        serviceScope.cancel()
    }
}