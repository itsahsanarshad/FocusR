//package com.focusr.v2
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.launch
//
//class BootReceiver : BroadcastReceiver() {
//    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//
//    override fun onReceive(context: Context, intent: Intent) {
//        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
//            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
//
//            val preferencesManager = PreferencesManager(context)
//
//            scope.launch {
//                val wasBlockingEnabled = preferencesManager.wasBlockingEnabledBeforeReboot.first()
//
//                if (wasBlockingEnabled) {
//                    // Restart the service
//                    val serviceIntent = Intent(context, AppMonitoringService::class.java)
//                    context.startForegroundService(serviceIntent)
//
//                    // Set toggle back to ON
//                    preferencesManager.setBlockingEnabled(true)
//                }
//            }
//        }
//    }
//}



package com.focusr.v2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val preferencesManager = PreferencesManager(context)
            val blockingTimeManager = BlockingTimeManager(preferencesManager)

            scope.launch {
                // Perform migration if needed
                preferencesManager.migrateOldBlockedApps()
                
                // Check if any rules are currently active
                val activeRulesCount = blockingTimeManager.getActiveRulesCount()

                if (activeRulesCount > 0) {
                    Log.d("BootReceiver", "Found $activeRulesCount active rules after boot, starting service")
                    
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            val serviceIntent = Intent(context, AppMonitoringService::class.java)
                            context.startForegroundService(serviceIntent)
                            Log.d("BootReceiver", "Service started successfully")
                        } catch (e: Exception) {
                            Log.e("BootReceiver", "Failed to start service: ${e.message}")
                        }
                    }, 3000) // Delay slightly after boot
                } else {
                    Log.d("BootReceiver", "No active rules after boot, service not started")
                }
            }
        }
    }
}
