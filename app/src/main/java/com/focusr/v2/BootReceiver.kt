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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val preferencesManager = PreferencesManager(context)

            scope.launch {
                val wasBlockingEnabled = preferencesManager.wasBlockingEnabledBeforeReboot.first()
                val advancedMode = preferencesManager.advancedMode.first()
                val fromTime = preferencesManager.fromTime.first() // Pair<Int, Int>
                val toTime = preferencesManager.toTime.first()     // Pair<Int, Int>

                val now = Calendar.getInstance()
                val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val fromMinutes = fromTime.first * 60 + fromTime.second
                val toMinutes = toTime.first * 60 + toTime.second

                val sessionStillValid = if (!advancedMode) {
                    currentMinutes in (fromMinutes..toMinutes)
                } else {
                    // Handle overnight: e.g. 10PM to 4AM
                    if (fromMinutes > toMinutes) {
                        currentMinutes >= fromMinutes || currentMinutes <= toMinutes
                    } else {
                        currentMinutes in (fromMinutes..toMinutes)
                    }
                }

                if (wasBlockingEnabled && sessionStillValid) {
                    preferencesManager.setBlockingEnabled(true)

                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            val serviceIntent = Intent(context, AppMonitoringService::class.java)
                            context.startForegroundService(serviceIntent)
                        } catch (e: Exception) {
                            try {
                                val fallbackIntent = Intent(context, AppMonitoringService::class.java)
                                context.startService(fallbackIntent)
                            } catch (e2: Exception) {
                                scope.launch {
                                    preferencesManager.setBlockingEnabled(false)
                                }
                            }
                        }
                    }, 3000) // Delay slightly after boot
                } else {
                    // Session expired – don't resume
                    preferencesManager.setBlockingEnabled(false)
                }
            }
        }
    }
}
