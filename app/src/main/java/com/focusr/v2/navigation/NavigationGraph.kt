//package com.focusr.v2.navigation
//
//import androidx.activity.ComponentActivity
//import androidx.compose.runtime.*
//import androidx.compose.ui.platform.LocalContext
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import com.focusr.v2.AppInfo
//import com.focusr.v2.AppManager
//import com.focusr.v2.PreferencesManager
//import com.focusr.v2.ui.screens.ModernAppSelectionScreen
//import com.focusr.v2.ui.screens.HomeScreen
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.runBlocking
//
//@Composable
//fun NavigationGraph(navController: NavHostController) {
//    val context = LocalContext.current
//    val preferencesManager = remember { PreferencesManager(context) }
//
//    // Installed apps
//    var allApps by remember { mutableStateOf(emptyList<AppInfo>()) }
//
//    // Blocked apps as a state
//    var blockedApps by remember {
//        mutableStateOf(emptySet<String>())
//    }
//
//    // Load once
//    LaunchedEffect(Unit) {
//        allApps = AppManager.getInstalledApps(context)
//        blockedApps = preferencesManager.blockedApps.first()
//    }
//
//    NavHost(navController = navController, startDestination = Screen.Home.route) {
//
//        composable(Screen.Home.route) {
//            val activity = LocalContext.current as ComponentActivity
//            HomeScreen(activity = activity,
//                navController = navController
//            )
//        }
//
//        composable(Screen.AppSelection.route) {
//            ModernAppSelectionScreen(
//                currentBlockedApps = blockedApps,
//                availableApps = allApps,
//                onBackClick = { navController.popBackStack() },
//                onAppsSelected = { selected ->
//                    runBlocking {
//                        preferencesManager.setBlockedApps(selected)
//                        blockedApps = selected
//                    }
//                    navController.popBackStack()
//                }
//            )
//        }
//    }
//}


package com.focusr.v2.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.focusr.v2.AppInfo
import com.focusr.v2.AppManager
import com.focusr.v2.PreferencesManager
import com.focusr.v2.ui.screens.ModernAppSelectionScreen
import com.focusr.v2.ui.screens.HomeScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    // Installed apps
    var allApps by remember { mutableStateOf(emptyList<AppInfo>()) }

    // Blocked apps as a state
    var blockedApps by remember {
        mutableStateOf(emptySet<String>())
    }

    // Load once
    LaunchedEffect(Unit) {
        allApps = AppManager.getInstalledApps(context)
        blockedApps = preferencesManager.blockedApps.first()
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {

        composable(Screen.Home.route) {
            val activity = LocalContext.current as ComponentActivity
            HomeScreen(
                activity = activity,
                navController = navController
            )
        }

        composable(Screen.AppSelection.route) {
            ModernAppSelectionScreen(
                currentBlockedApps = blockedApps,
                availableApps = allApps,
                onBackClick = { navController.popBackStack() },
                onAppsSelected = { selected ->
                    runBlocking {
                        preferencesManager.setBlockedApps(selected)
                        blockedApps = selected
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}