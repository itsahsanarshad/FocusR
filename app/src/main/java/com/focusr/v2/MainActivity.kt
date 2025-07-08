package com.focusr.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.focusr.v2.navigation.NavigationGraph
import com.focusr.v2.ui.screens.HomeScreen
import com.focusr.v2.ui.theme.OpalForAndroidTheme

class MainActivity : ComponentActivity() {
    private var onResumeCallback: (() -> Unit)? = null

    fun setOnResumeCallback(callback: () -> Unit) {
        onResumeCallback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OpalForAndroidTheme(dynamicColor = false, darkTheme = false) {
//                HomeScreen(activity = this@MainActivity)
                val navController = rememberNavController()
                NavigationGraph(navController = navController)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        onResumeCallback?.invoke()
    }
}
