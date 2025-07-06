//package com.focusr.v2
//import android.accessibilityservice.AccessibilityService
//import android.content.Intent
//import android.view.accessibility.AccessibilityEvent
//import android.widget.Toast
//import kotlinx.coroutines.*
//import kotlinx.coroutines.flow.first
//import java.util.*
//
//class FocusBlockerAccessibilityService : AccessibilityService() {
//
//    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
//
//    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
//
//        val packageName = event.packageName?.toString() ?: return
//
//        serviceScope.launch {
//            val prefs = PreferencesManager(applicationContext)
//
//            val isBlocking = prefs.blockingEnabled.first()
//            if (!isBlocking) return@launch
//
//            val blockedApps = prefs.blockedApps.first()
//            if (!blockedApps.contains(packageName)) return@launch
//
//            val fromTime = prefs.fromTime.first()
//            val toTime = prefs.toTime.first()
//            val isInBlockedWindow = isInBlockedTime(fromTime.first, fromTime.second, toTime.first, toTime.second)
//
////            if (isInBlockedWindow) {
////                withContext(Dispatchers.Main) {
//////                    Toast.makeText(applicationContext, "Focus time active - $packageName blocked!", Toast.LENGTH_SHORT).show()
////
////                    val intent = Intent(applicationContext, BlockerOverlayActivity::class.java).apply {
////                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
////                    }
////                    startActivity(intent)
////                }
////            }
//        }
//    }
//
//    override fun onInterrupt() {}
//
//    override fun onDestroy() {
//        super.onDestroy()
//        serviceScope.cancel()
//    }
//
//    private fun isInBlockedTime(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): Boolean {
//        val now = Calendar.getInstance()
//        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
//        val startMinutes = startHour * 60 + startMinute
//        val endMinutes = endHour * 60 + endMinute
//
//        return if (startMinutes <= endMinutes) {
//            nowMinutes in startMinutes until endMinutes
//        } else {
//            // Over midnight
//            nowMinutes >= startMinutes || nowMinutes < endMinutes
//        }
//    }
//}
