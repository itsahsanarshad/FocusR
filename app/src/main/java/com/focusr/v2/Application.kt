//
//
//package com.focusr.v2
//
//
//import android.app.Application
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.launch
//
//class MyApplication : Application() {
//    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//
//    override fun onCreate() {
//        super.onCreate()
//
//        // Turn off toggle when app process starts
//        val preferencesManager = PreferencesManager(this)
//        applicationScope.launch {
//            preferencesManager.setBlockingEnabled(false)
//        }
//    }
//}

package com.focusr.v2

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

//class MyApplication : Application() {
//    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//
//    override fun onCreate() {
//        super.onCreate()
//
//        // Check if we're currently in a blocking session and start service if needed
//        val preferencesManager = PreferencesManager(this)
//        val blockingTimeManager = BlockingTimeManager(preferencesManager)
//
//        applicationScope.launch {
//            if (blockingTimeManager.isCurrentlyInBlockingSession()) {
//                // Enable blocking and start the monitoring service
//                preferencesManager.setBlockingEnabled(true)
//                preferencesManager.setBlockingStartTime(System.currentTimeMillis())
//
//                // Start the app monitoring service
//                startAppMonitoringService()
//
//                // Show confirmation toast on main thread
//                launch(Dispatchers.Main) {
//                    Toast.makeText(this@MyApplication, "FocusR Service Started.", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//
//    private fun startAppMonitoringService() {
//        try {
//            val serviceIntent = Intent(this, AppMonitoringService::class.java)
//
//            // Start as foreground service for better reliability
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForegroundService(serviceIntent)
//            } else {
//                startService(serviceIntent)
//            }
//        } catch (e: Exception) {
//            // Log error or handle service start failure
//            Log.e("MyApplication", "Failed to start monitoring service", e)
//        }
//    }
//
//    override fun onTerminate() {
//        super.onTerminate()
//        // Cancel any ongoing coroutines
//        applicationScope.cancel()
//    }
//}


class MyApplication : Application() {
    private lateinit var serviceManager: ServiceManager

    override fun onCreate() {
        super.onCreate()
        serviceManager = ServiceManager(this)

        CoroutineScope(Dispatchers.IO).launch {
            val preferencesManager = PreferencesManager(this@MyApplication)
            val blockingTimeManager = BlockingTimeManager(preferencesManager)
            val isBlockingEnabled = preferencesManager.blockingEnabled.first() // user toggle
            val inSession = blockingTimeManager.isCurrentlyInBlockingSession()

            if (isBlockingEnabled && inSession) {
                serviceManager.toggleBlocking(true)
            }
        }
    }
}
