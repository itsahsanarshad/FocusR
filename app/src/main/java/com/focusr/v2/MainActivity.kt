


//package com.focusr.v2
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.statusBarsPadding
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Brush
//import androidx.core.view.WindowCompat
//import androidx.core.view.WindowInsetsCompat
//import androidx.core.view.WindowInsetsControllerCompat
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.rememberNavController
//import com.focusr.v2.navigation.NavigationGraph
//import com.focusr.v2.ui.components.BottomNavBar
//import com.focusr.v2.ui.theme.OpalForAndroidTheme
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.launch
//import java.util.*
//
//class MainActivity : ComponentActivity() {
//    private var onResumeCallback: (() -> Unit)? = null
//    private lateinit var preferencesManager: PreferencesManager
//    private lateinit var serviceScheduler: ServiceScheduler
//
//    fun setOnResumeCallback(callback: () -> Unit) {
//        onResumeCallback = callback
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Initialize preferences and service scheduler
//        preferencesManager = PreferencesManager(this)
//        serviceScheduler = ServiceScheduler(this)
//
//        // Enable edge-to-edge display
//        enableEdgeToEdge()
//
//        // Configure window insets controller
//        WindowCompat.setDecorFitsSystemWindows(window, false)
//        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
//        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//
//        // Hide only navigation bar, keep status bar for notched devices
//        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
//
//        setContent {
//            OpalForAndroidTheme(dynamicColor = false, darkTheme = false) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(
//                            brush = Brush.verticalGradient(
//                                colors = listOf(
//                                    Color(0xFF1A1A2E),
//                                    Color(0xFF16213E),
//                                    Color(0xFF0F3460)
//                                )
//                            )
//                        )
//                ) {
//                    val navController = rememberNavController()
//                    MainScreen(navController = navController)
//                }
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        // Re-hide navigation bar when app comes back to foreground
//        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
//        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
//        onResumeCallback?.invoke()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        // Clean up if needed
//    }
//
//    // Function to handle blocking toggle from UI components
//    fun onBlockingToggled(enabled: Boolean) {
//        lifecycleScope.launch {
//            preferencesManager.setBlockingEnabled(enabled)
//            if (enabled) {
//                // Schedule service to start at appropriate time
//                serviceScheduler.scheduleService()
//                // Show user when blocking will start
//                showBlockingScheduleMessage()
//            } else {
//                // Stop service and cancel scheduled starts
//                serviceScheduler.cancelScheduledService()
//                stopService(Intent(this@MainActivity, AppMonitoringService::class.java))
//                Toast.makeText(this@MainActivity, "Blocking disabled", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private suspend fun showBlockingScheduleMessage() {
//        val advancedMode = preferencesManager.advancedMode.first()
//        val fromTime = preferencesManager.fromTime.first()
//        val toTime = preferencesManager.toTime.first()
//
//        if (!advancedMode) {
//            val now = Calendar.getInstance()
//            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
//            val toMinutes = toTime.first * 60 + toTime.second
//
//            if (currentMinutes <= toMinutes) {
//                Toast.makeText(
//                    this,
//                    "Blocking started until ${toTime.first}:${toTime.second.toString().padStart(2, '0')}",
//                    Toast.LENGTH_LONG
//                ).show()
//            } else {
//                Toast.makeText(
//                    this,
//                    "Blocking scheduled for tomorrow until ${toTime.first}:${toTime.second.toString().padStart(2, '0')}",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        } else {
//            val now = Calendar.getInstance()
//            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
//            val fromMinutes = fromTime.first * 60 + fromTime.second
//            val toMinutes = toTime.first * 60 + toTime.second
//
//            val isCurrentlyInBlockingTime = if (fromMinutes > toMinutes) {
//                // Overnight case
//                currentMinutes >= fromMinutes || currentMinutes <= toMinutes
//            } else {
//                // Same day case
//                currentMinutes in fromMinutes..toMinutes
//            }
//
//            val message = if (isCurrentlyInBlockingTime) {
//                "Blocking is now active until ${toTime.first}:${toTime.second.toString().padStart(2, '0')}"
//            } else {
//                "Blocking will start at ${fromTime.first}:${fromTime.second.toString().padStart(2, '0')}"
//            }
//
//            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
//        }
//    }
//}
//
//@Composable
//fun MainScreen(navController: NavHostController) {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .statusBarsPadding() // Essential for notched devices
//    ) {
//        // Main content area - takes up remaining space
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .weight(1f) // Takes up all available space except bottom nav
//        ) {
//            NavigationGraph(
//                navController = navController,
//                modifier = Modifier.fillMaxSize()
//            )
//        }
//
//        // Bottom navigation bar
//        BottomNavBar(navController = navController)
//    }
//}



package com.focusr.v2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.focusr.v2.navigation.NavigationGraph
import com.focusr.v2.ui.components.BottomNavBar
import com.focusr.v2.ui.theme.OpalForAndroidTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var onResumeCallback: (() -> Unit)? = null
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var serviceScheduler: ServiceScheduler
    private lateinit var blockingTimeManager: BlockingTimeManager

    fun setOnResumeCallback(callback: () -> Unit) {
        onResumeCallback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize preferences and managers
        preferencesManager = PreferencesManager(this)
        serviceScheduler = ServiceScheduler(this)
        blockingTimeManager = BlockingTimeManager(preferencesManager)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Configure window insets controller
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Hide only navigation bar, keep status bar for notched devices
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())

        setContent {
            OpalForAndroidTheme(dynamicColor = false, darkTheme = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1A1A2E),
                                    Color(0xFF16213E),
                                    Color(0xFF0F3460)
                                )
                            )
                        )
                ) {
                    val navController = rememberNavController()
                    MainScreen(navController = navController)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-hide navigation bar when app comes back to foreground
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        onResumeCallback?.invoke()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up if needed
    }



}

@Composable
fun MainScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // Essential for notched devices
    ) {
        // Main content area - takes up remaining space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Takes up all available space except bottom nav
        ) {
            NavigationGraph(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom navigation bar
        BottomNavBar(navController = navController)
    }
}