//package com.focusr.v2
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.navigation.compose.rememberNavController
//import com.focusr.v2.navigation.NavigationGraph
//import com.focusr.v2.ui.screens.HomeScreen
//import com.focusr.v2.ui.theme.OpalForAndroidTheme
//
//class MainActivity : ComponentActivity() {
//    private var onResumeCallback: (() -> Unit)? = null
//
//    fun setOnResumeCallback(callback: () -> Unit) {
//        onResumeCallback = callback
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        setContent {
//            OpalForAndroidTheme(dynamicColor = false, darkTheme = false) {
////                HomeScreen(activity = this@MainActivity)
//                val navController = rememberNavController()
//                NavigationGraph(navController = navController)
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        onResumeCallback?.invoke()
//    }
//}



//package com.focusr.v2
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.rememberNavController
//import com.focusr.v2.navigation.NavigationGraph
//import com.focusr.v2.ui.components.BottomNavBar
//import com.focusr.v2.ui.theme.OpalForAndroidTheme
//
//class MainActivity : ComponentActivity() {
//    private var onResumeCallback: (() -> Unit)? = null
//
//    fun setOnResumeCallback(callback: () -> Unit) {
//        onResumeCallback = callback
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        setContent {
//            OpalForAndroidTheme(dynamicColor = false, darkTheme = false) {
//                // Remove Surface and use Box with gradient background
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
//        onResumeCallback?.invoke()
//    }
//}
//
//@Composable
//fun MainScreen(navController: NavHostController) {
//    Scaffold(
//        modifier = Modifier.fillMaxSize(),
//        containerColor = Color.Transparent, // Make scaffold transparent
//        bottomBar = {
//            BottomNavBar(navController = navController)
//        }
//    ) { innerPadding ->
//        NavigationGraph(
//            navController = navController,
//            modifier = Modifier.padding(innerPadding)
//        )
//    }
//}

package com.focusr.v2

import android.os.Bundle
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.focusr.v2.navigation.NavigationGraph
import com.focusr.v2.ui.components.BottomNavBar
import com.focusr.v2.ui.theme.OpalForAndroidTheme

class MainActivity : ComponentActivity() {
    private var onResumeCallback: (() -> Unit)? = null

    fun setOnResumeCallback(callback: () -> Unit) {
        onResumeCallback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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