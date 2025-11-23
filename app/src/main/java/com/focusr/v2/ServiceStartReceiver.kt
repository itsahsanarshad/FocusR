package com.focusr.v2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Build

class ServiceStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ServiceStartReceiver", "=== ALARM TRIGGERED ===")
        Log.d("ServiceStartReceiver", "Starting monitoring service")
        
        val serviceIntent = Intent(context, AppMonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        
        Log.d("ServiceStartReceiver", "Service start command sent")
    }
}