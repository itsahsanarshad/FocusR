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
import java.util.*
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class AppMonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var monitoringRunnable: Runnable? = null
    private lateinit var preferencesManager: PreferencesManager
    private var hasDayChanged = false
    private var hasExceededTime = false
    private var hasBlockingStarted = false  // ✅ Local flag (not persisted)
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
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        startForeground(NOTIFICATION_ID, buildNotification())
//        startMonitoring()
//        return START_STICKY
//
//
//    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
            startNotificationUpdates() // ⬅ start loop
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
            val blockingEnabled = preferencesManager.blockingEnabled.first()
            if (!blockingEnabled) return

            val blockedApps = preferencesManager.blockedApps.first()
            if (blockedApps.isEmpty()) return

            // ✅ Step 1: Capture the current foreground app BEFORE anything
            val currentApp = getCurrentForegroundApp()
            lastDetectedApp = currentApp  // ✅ Save it globally
            Log.d("blockerdebugams", "Foreground app detected: $currentApp")
            delay(100)

            // ✅ Step 2: Only block AFTER verifying
            if (currentApp != null && blockedApps.contains(currentApp)) {
                if (isInBlockingHours()) {
                    blockApp(currentApp)  // We pass the app that was foreground BEFORE overlay launched
                }
            }

            if (hasExceededToTime()) {
                preferencesManager.setBlockingEnabled(false)
                stopSelf()
                return
            }

        } catch (e: Exception) {
            Log.e("BlockerDebug", "Error: ${e.message}")
        }
    }

//    private suspend fun checkCurrentApp() {
//        try {
//            val blockingEnabled = preferencesManager.blockingEnabled.first()
//
//
//
//            if (!blockingEnabled) return
//
//            val blockedApps = preferencesManager.blockedApps.first()
//            if (blockedApps.isEmpty()) return
//
//            val currentApp = getCurrentForegroundApp()
//            if (currentApp != null && blockedApps.contains(currentApp)) {
//                if (isInBlockingHours()) {
//                    blockApp(currentApp)
//                }
//
//            }
//
//            // 🔴 Check time first before doing anything
//            if (hasExceededToTime()) {
//                preferencesManager.setBlockingEnabled(false)
//                stopSelf()
//                return // 💡 Don't continue if time is exceeded
//            }
//
//
//        } catch (e: Exception) {
//            // Handle errors silently
//        }
//    }

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

    private suspend fun isInBlockingHours(): Boolean {
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



//    private suspend fun hasExceededToTime(): Boolean {
//        val advancedMode = preferencesManager.advancedMode.first()
//        val fromTime = preferencesManager.fromTime.first()
//        val toTime = preferencesManager.toTime.first()
//
//        val now = Calendar.getInstance()
//        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
//        val fromMinutes = fromTime.first * 60 + fromTime.second
//        val toMinutes = toTime.first * 60 + toTime.second
//
//        return if (!advancedMode) {
//            // 🔹 Simple mode: time has exceeded if current > toTime
//            currentMinutes > toMinutes
//        } else {
//            // 🔸 Advanced mode: Handle overnight logic
//            if (fromMinutes > toMinutes) {
//                // Overnight scenario (e.g., 10 PM to 2 AM)
//
//                // Set hasDayChanged flag based on current time
//                // If current time is between 00:00 and toTime, we're in "after midnight" phase
//                if (currentMinutes <= toMinutes && !hasDayChanged) {
//                    hasDayChanged = true
//                }
//                // Also set flag when exactly at midnight
//                if (currentMinutes == 0 && !hasDayChanged) {
//                    hasDayChanged = true
//                }
//
//                // If day hasn't changed yet, we're still in the "before midnight" phase
//                if (!hasDayChanged) {
//                    // We're in the first part (10 PM - 11:59 PM), never exceeded
//                    false
//                } else {
//                    // Day has changed, we're in the "after midnight" phase (12:00 AM - 2 AM)
//                    // Time exceeded when current time > toTime
//                    currentMinutes > toMinutes
//                }
//            } else {
//                // Same-day scenario (e.g., 9 AM to 5 PM)
//                // Simple check: current time > toTime
//                currentMinutes > toMinutes
//            }
//        }
//    }

    private suspend fun hasExceededToTime(): Boolean { //looks good test
        val advancedMode = preferencesManager.advancedMode.first()
        val fromTime = preferencesManager.fromTime.first()
        val toTime = preferencesManager.toTime.first()
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val fromMinutes = fromTime.first * 60 + fromTime.second
        val toMinutes = toTime.first * 60 + toTime.second
        Log.d("appmon ser","fromMinutes $fromMinutes, to min $toMinutes, cur mint $currentMinutes")

        if (!advancedMode) {
            // 🔹 Simple mode: time has exceeded if current > toTime
            return currentMinutes > toMinutes
        }

        // 🔸 Advanced mode: Handle all scenarios separately

        // Case 1: Overnight scenario (fromMinutes > toMinutes)
        if (fromMinutes > toMinutes) {
            Log.d("appmon ser", "Overnight scenario detected")

            // Reset hasDayChanged when we're back in the "from" period
            if (currentMinutes >= fromMinutes && hasDayChanged) {
                hasDayChanged = false
                Log.d("appmon ser", "Day changed flag reset - new cycle starting")
            }

            // Set hasDayChanged flag based on current time
            if (currentMinutes <= toMinutes && !hasDayChanged) {
                hasDayChanged = true
                Log.d("appmon ser", "Day changed flag set to true")
            }

            // Also set flag when exactly at midnight
            if (currentMinutes == 0 && !hasDayChanged) {
                hasDayChanged = true
                Log.d("appmon ser", "Day changed flag set to true at midnight")
            }

            // Time is exceeded only when we're past toTime AND day has changed
            if (hasDayChanged && currentMinutes > toMinutes) {
                Log.d("appmon ser", "Overnight session exceeded")
                return true
            } else {
                Log.d("appmon ser", "Overnight session active")
                return false
            }
        }

        // Case 2: Same day or Future block scenario (fromMinutes <= toMinutes)

        // Reset hasBlockingStarted when we're outside the blocking window
        if (currentMinutes < fromMinutes || currentMinutes > toMinutes) {
            if (hasBlockingStarted) {
                hasBlockingStarted = false
                Log.d("appmon ser", "Blocking session flag reset - outside blocking window")
                return true
            }
        }

        // Set blocking started flag when we reach fromTime
        if (currentMinutes >= fromMinutes && currentMinutes <= toMinutes && !hasBlockingStarted) {
            hasBlockingStarted = true
            Log.d("appmon ser", "Blocking session started")
        }

        // Case 2a: Blocking session has started
        if (hasBlockingStarted) {
            Log.d("appmon ser", "Checking if blocking session exceeded")
            if (currentMinutes > toMinutes) {
                Log.d("appmon ser", "Blocking session exceeded - stopping service")
                return true
            } else {
                Log.d("appmon ser", "Blocking session active")
                return false
            }
        }

        // Case 2b: Before blocking window or after blocking window
        Log.d("appmon ser", "Outside blocking window - no blocking active")
        return false
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

    private suspend fun getDynamicNotificationMessage(): String {
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


    private suspend fun buildNotification(): Notification {
        val message = getDynamicNotificationMessage()
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
                notificationHandler.postDelayed(this, 10000) // Update every 30sec
            }
        }
        notificationHandler.post(notificationRunnable!!)
    }

    private fun stopNotificationUpdates() {
        notificationRunnable?.let { notificationHandler.removeCallbacks(it) }
    }





    // Updated onDestroy method to reset flags
    override fun onDestroy() {
        super.onDestroy()
        // Reset flags when service stops
        hasDayChanged = false
        hasExceededTime = false
        hasBlockingStarted = false  // ✅ Reset session flag

        stopNotificationUpdates()
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        serviceScope.cancel()


    }
}