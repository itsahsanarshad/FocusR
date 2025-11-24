package com.focusr.v2

import android.util.Log
import kotlinx.coroutines.flow.first
import java.util.*

class BlockingTimeManager(private val preferencesManager: PreferencesManager) {

    // ========== NEW RULE-BASED SYSTEM ==========
    
    /**
     * Check if a specific app should be blocked based on its rules
     */
   suspend fun shouldBlockApp(packageName: String): Boolean {

    // Check if paused - don't block any apps when paused
    val pauseUntil = preferencesManager.pauseUntil.first()
    if (pauseUntil != null && pauseUntil > System.currentTimeMillis()) {
        Log.d("BlockingTimeManager", "⏸ PAUSED - Not blocking $packageName")
        return false
    }
    
    val allRules = preferencesManager.blockingRules.first()
    
    Log.d("BlockingTimeManager", "=== Checking $packageName ===")
    Log.d("BlockingTimeManager", "Total rules: ${allRules.size}")
    
    // Get all rules that apply to this app
    val applicableRules = allRules.filter { rule ->
        rule.enabled && rule.getApps().contains(packageName)
    }.sortedBy { it.calculatePriority() }
    
    Log.d("BlockingTimeManager", "Applicable rules: ${applicableRules.size}")
    
    // Check rules in priority order
    for (rule in applicableRules) {
        val isActive = isRuleActive(rule)
        Log.d("BlockingTimeManager", "Rule '${rule.getDisplayName()}' (${rule.ruleType}): active=$isActive")
        if (isActive) {
            Log.d("BlockingTimeManager", "✓ BLOCKING $packageName")
            return true
        }
    }
    
    Log.d("BlockingTimeManager", "✗ NOT blocking $packageName")
    return false
}
    
    /**
     * Check if a specific rule is currently active
     */
     fun isRuleActive(rule: com.focusr.v2.models.BlockingRule): Boolean {
        return when (rule.ruleType) {
            com.focusr.v2.models.RuleType.SIMPLE -> checkSimpleRule(rule)
            com.focusr.v2.models.RuleType.SCHEDULED -> checkScheduledRule(rule)
        }
    }
    
    /**
     * Check if a SIMPLE rule is active (block until time today)
     */
    private fun checkSimpleRule(rule: com.focusr.v2.models.BlockingRule): Boolean {
        val blockUntilTime = rule.blockUntilTime ?: return false
        
        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val blockUntilMinutes = blockUntilTime.first * 60 + blockUntilTime.second
        
        // Block if current time is before the "block until" time
        return currentMinutes <= blockUntilMinutes
    }
    
    /**
     * Check if a SCHEDULED rule is active (recurring schedule)
     */
    private fun checkScheduledRule(rule: com.focusr.v2.models.BlockingRule): Boolean {
        val fromTime = rule.fromTime ?: return false
        val toTime = rule.toTime ?: return false
        
        val calendar = Calendar.getInstance()
        val currentDay = com.focusr.v2.models.DayOfWeek.getCurrentDay()
        
        // Check if today is in the rule's active days
        if (!rule.daysOfWeek.contains(currentDay)) {
            return false
        }
        
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val fromMinutes = fromTime.first * 60 + fromTime.second
        val toMinutes = toTime.first * 60 + toTime.second
        
        return if (fromMinutes > toMinutes) {
            // Overnight schedule (e.g., 10 PM to 2 AM)
            currentMinutes >= fromMinutes || currentMinutes <= toMinutes
        } else {
            // Same-day schedule (e.g., 9 AM to 5 PM)
            currentMinutes in fromMinutes..toMinutes
        }
    }
    
    /**
     * Get count of currently active rules
     */
    suspend fun getActiveRulesCount(): Int {
        val allRules = preferencesManager.getAllActiveRules()
        return allRules.count { isRuleActive(it) }
    }
    
    /**
     * Get all apps that should currently be blocked
     */
suspend fun getCurrentlyBlockedApps(): Set<String> {
    val allRules = preferencesManager.blockingRules.first()
    return allRules
        .filter { it.enabled && isRuleActive(it) }
        .flatMap { it.getApps() }  // Use getApps() instead!
        .toSet()
}
}