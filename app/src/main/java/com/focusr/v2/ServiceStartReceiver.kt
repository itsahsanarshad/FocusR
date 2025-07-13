package com.focusr.v2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ServiceStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ServiceStartReceiver", "Starting monitoring service")
        val serviceIntent = Intent(context, AppMonitoringService::class.java)
        context.startForegroundService(serviceIntent)
    }
}