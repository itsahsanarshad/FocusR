package com.focusr.v2.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.focusr.v2.AppInfo
import com.focusr.v2.AppManager
import com.focusr.v2.BlockingTimeManager
import com.focusr.v2.MainActivity
import com.focusr.v2.PreferencesManager
import com.focusr.v2.ui.screens.ModernAppSelectionScreen
import com.focusr.v2.ui.screens.HomeScreen
import com.focusr.v2.ui.screens.RuleEditorScreen
import com.focusr.v2.ui.screens.ManageRulesScreen
import com.focusr.v2.ui.viewmodels.RuleViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val blockingTimeManager = remember { BlockingTimeManager(preferencesManager) }
    
    // Shared RuleViewModel
   val ruleViewModel: RuleViewModel = viewModel {
    RuleViewModel(preferencesManager, blockingTimeManager, context)  // Added context
}

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
            val activity = LocalContext.current as MainActivity
            HomeScreen(
                activity = activity,
                navController = navController
            )
        }

        composable(Screen.ManageRules.route) {
        ManageRulesScreen(
            navController = navController,
            ruleViewModel = ruleViewModel
        )
    }

        composable(Screen.AppSelection.route) {
            ModernAppSelectionScreen(
                onBackClick = { navController.popBackStack() },
                navController = navController,
                ruleViewModel = ruleViewModel,
                availableApps = allApps
            )
        }
        
        // RuleEditor route
       // RuleEditor route - UPDATED
composable(
    route = Screen.RuleEditor.route,
    arguments = listOf(
        navArgument("ruleId") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }
    )
) { backStackEntry ->
    val ruleId = backStackEntry.arguments?.getString("ruleId")
    RuleEditorScreen(
        ruleId = ruleId,
        ruleViewModel = ruleViewModel,
        navController = navController,
        availableApps = allApps
    )
}
    }
}