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
            com.focusr.v2.models.RuleType.MENTAL_CLARITY -> checkMentalClarityRule(rule)
        }
    }
    
    /**
     * Check if a SIMPLE rule is active (duration-based)
     */
    private fun checkSimpleRule(rule: com.focusr.v2.models.BlockingRule): Boolean {
        val durationMinutes = rule.durationMinutes ?: return false
        val activatedAt = rule.activatedAt ?: return false
        
        val expiresAt = activatedAt + (durationMinutes * 60 * 1000L)
        return System.currentTimeMillis() < expiresAt
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
     * Check if a MENTAL_CLARITY rule is active.
     * This rule has two phases:
     * 1. Wind-down: After set time, blocks apps until next morning
     * 2. Morning Fury: After wake-up detected, blocks apps for set duration
     */
    private fun checkMentalClarityRule(rule: com.focusr.v2.models.BlockingRule): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinutes = currentHour * 60 + calendar.get(Calendar.MINUTE)
        val now = System.currentTimeMillis()
        
        // Check if today is in the rule's active days
        val currentDay = com.focusr.v2.models.DayOfWeek.getCurrentDay()
        if (!rule.daysOfWeek.contains(currentDay)) {
            return false
        }
        
        // Phase 1: Wind-down check
        if (rule.windDownEnabled && rule.windDownTime != null) {
            val windDownMinutes = rule.windDownTime.first * 60 + rule.windDownTime.second
            val morningWindowEnd = rule.morningWindowEnd
            
            // Active from wind-down time until morning window starts
            // e.g., 9 PM until 4 AM next day
            if (currentMinutes >= windDownMinutes || currentHour < rule.morningWindowStart) {
                Log.d("BlockingTimeManager", "Mental Clarity: Wind-down phase active")
                return true
            }
        }
        
        // Phase 2: Morning Fury check
        if (rule.morningFuryEnabled && rule.morningBlockDuration != null) {
            // Check if wake-up was detected and we're still in the blocking window
            val wakeUpAt = rule.wakeUpDetectedAt
            if (wakeUpAt != null) {
                val expiresAt = wakeUpAt + (rule.morningBlockDuration * 60 * 1000L)
                if (now < expiresAt) {
                    Log.d("BlockingTimeManager", "Mental Clarity: Morning Fury phase active")
                    return true
                }
            }
        }
        
        return false
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