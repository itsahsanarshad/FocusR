package com.focusr.v2.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AppSelection : Screen("app_selection")
}
