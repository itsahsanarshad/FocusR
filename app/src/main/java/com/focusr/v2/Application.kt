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
package com.focusr.v2

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MyApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val preferencesManager = PreferencesManager(this)
        val blockingTimeManager = BlockingTimeManager(preferencesManager)

        applicationScope.launch {
            // Perform migration from old system to new rule-based system
            preferencesManager.migrateOldBlockedApps()
            
            // Check if any rules are currently active
            val activeRulesCount = blockingTimeManager.getActiveRulesCount()
            
            if (activeRulesCount > 0) {
                Log.d("MyApplication", "Found $activeRulesCount active rules, starting service")
                // Start the app monitoring service
                startAppMonitoringService()
            } else {
                Log.d("MyApplication", "No active rules, service not started")
            }
        }
    }

    private fun startAppMonitoringService() {
        try {
            val serviceIntent = Intent(this, AppMonitoringService::class.java)

            // Start as foreground service for better reliability
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            Log.d("MyApplication", "AppMonitoringService started successfully")
        } catch (e: Exception) {
            Log.e("MyApplication", "Failed to start monitoring service", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Cancel any ongoing coroutines
        applicationScope.cancel()
    }
}