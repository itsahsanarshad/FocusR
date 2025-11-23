package com.focusr.v2.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    //object AppSelection : Screen("app_selection")
    object ManageRules : Screen("manage_rules")
    
    object RuleEditor : Screen("rule_editor?ruleId={ruleId}") {
        fun createRoute(ruleId: String? = null): String {
            return if (ruleId != null) {
                "rule_editor?ruleId=$ruleId"
            } else {
                "rule_editor"
            }
        } 
    }
    object AppSelection : Screen("app_selection?ruleId={ruleId}") {
    fun createRoute(ruleId: String? = null): String {
    return if (ruleId != null) {
        "app_selection?ruleId=$ruleId"
    } else {
        "app_selection"
    }
}
}
}
