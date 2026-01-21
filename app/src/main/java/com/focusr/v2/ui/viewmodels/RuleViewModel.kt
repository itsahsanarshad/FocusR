package com.focusr.v2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusr.v2.BlockingTimeManager
import com.focusr.v2.PreferencesManager
import com.focusr.v2.models.BlockingRule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RuleUiState(
    val allRules: List<BlockingRule> = emptyList(),
    val activeRulesCount: Int = 0,
    val blockedAppsCount: Int = 0,
    val isLoading: Boolean = false,
    val isPaused: Boolean = false,
    val pauseUntil: Long? = null
)

class RuleViewModel(
    private val preferencesManager: PreferencesManager,
    private val blockingTimeManager: BlockingTimeManager,
    private val context: android.content.Context  // NEW
) : ViewModel() {

    private val _uiState = MutableStateFlow(RuleUiState())
    val uiState: StateFlow<RuleUiState> = _uiState.asStateFlow()

    init {
        // Collect rules and pause state, update UI
        viewModelScope.launch {
            combine(
                preferencesManager.blockingRules,
                preferencesManager.pauseUntil
            ) { rules, pauseUntil ->
                val activeCount = blockingTimeManager.getActiveRulesCount()
                val blockedCount = blockingTimeManager.getCurrentlyBlockedApps().size
                val isPaused = pauseUntil != null && pauseUntil > System.currentTimeMillis()
                
                RuleUiState(
                    allRules = rules,
                    activeRulesCount = activeCount,
                    blockedAppsCount = blockedCount,
                    isLoading = false,
                    isPaused = isPaused,
                    pauseUntil = pauseUntil
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    /**
     * Schedule service based on current rules
     */
    private fun scheduleServiceIfNeeded() {
        viewModelScope.launch {
            val scheduler = com.focusr.v2.ServiceScheduler(context)
            scheduler.scheduleService()
        }
    }

    /**
     * Get all rules for a specific app (updated for multi-app support)
     */
    fun getRulesForApp(packageName: String): Flow<List<BlockingRule>> {
        return uiState.map { state ->
            state.allRules.filter { it.getApps().contains(packageName) }
        }
    }

    /**
     * Add a new blocking rule
     */
  fun addRule(rule: BlockingRule) {
    viewModelScope.launch {
        preferencesManager.addRule(rule)
        scheduleServiceIfNeeded()  // NEW
    }
}
    
    /**
     * Add or update a rule (convenience method)
     */
  fun addOrUpdateRule(rule: BlockingRule) {
    viewModelScope.launch {
        val existingRule = _uiState.value.allRules.find { it.id == rule.id }
        if (existingRule != null) {
            preferencesManager.updateRule(rule)
        } else {
            preferencesManager.addRule(rule)
        }
        scheduleServiceIfNeeded()  // NEW
    }
}

    /**
     * Update an existing rule
     */
    fun updateRule(rule: BlockingRule) {
        viewModelScope.launch {
            preferencesManager.updateRule(rule)
        }
    }

    /**
     * Delete a rule by ID
     */
    fun deleteRule(ruleId: String) {
    viewModelScope.launch {
        preferencesManager.removeRule(ruleId)
        scheduleServiceIfNeeded()  // NEW
    }
}

    /**
     * Toggle rule enabled/disabled
     * For SIMPLE rules: when enabling, reset activatedAt to restart the timer
     */
    fun toggleRuleEnabled(ruleId: String) {
        viewModelScope.launch {
            val rule = _uiState.value.allRules.find { it.id == ruleId }
            rule?.let {
                val updatedRule = if (!it.enabled && it.ruleType == com.focusr.v2.models.RuleType.SIMPLE) {
                    // Enabling a SIMPLE rule - reset the activation time to restart timer
                    it.copy(enabled = true, activatedAt = System.currentTimeMillis())
                } else {
                    // Just toggle enabled state
                    it.copy(enabled = !it.enabled)
                }
                preferencesManager.updateRule(updatedRule)
                scheduleServiceIfNeeded()
            }
        }
    }

    /**
     * Check if an app should currently be blocked
     */
    suspend fun shouldBlockApp(packageName: String): Boolean {
        return blockingTimeManager.shouldBlockApp(packageName)
    }
    
    /**
     * Get a specific rule by ID
     */
    fun getRuleById(ruleId: String): BlockingRule? {
        return _uiState.value.allRules.find { it.id == ruleId }
    }
    
    // ========== PAUSE FUNCTIONALITY ==========
    
    /**
     * Pause all rules for a specified duration
     * @param durationMinutes Duration in minutes, null for indefinite pause
     */
    fun pauseAllRules(durationMinutes: Int?) {
        viewModelScope.launch {
            val pauseUntil = if (durationMinutes != null) {
                System.currentTimeMillis() + (durationMinutes * 60 * 1000)
            } else {
                Long.MAX_VALUE // Paused until manual resume
            }
            preferencesManager.setPauseUntil(pauseUntil)
            
            // Stop service
            val scheduler = com.focusr.v2.ServiceScheduler(context)
            scheduler.cancelScheduledService()
            val intent = android.content.Intent(context, com.focusr.v2.AppMonitoringService::class.java)
            context.stopService(intent)
        }
    }
    
    /**
     * Resume all rules (unpause)
     */
    fun resumeAllRules() {
        viewModelScope.launch {
            preferencesManager.setPauseUntil(null)
            // Service will auto-start based on active rules
            scheduleServiceIfNeeded()
        }
    }
    
    /**
     * Get next rule activation time as formatted string
     * @return Formatted string like "Study Mode in 2h 15m" or null if no upcoming rules
     */
    fun getNextRuleActivation(): String? {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        val currentMinutes = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + 
                           calendar.get(java.util.Calendar.MINUTE)
        
        // Find next rule that will activate or show remaining time for active SIMPLE rules
        val upcomingRules = _uiState.value.allRules
            .filter { it.enabled }
            .mapNotNull { rule ->
                when (rule.ruleType) {
                    com.focusr.v2.models.RuleType.SIMPLE -> {
                        // For SIMPLE rules, show remaining time if active
                        val remaining = rule.getRemainingMinutes()
                        if (remaining != null && remaining > 0) {
                            Pair(rule, remaining)
                        } else null
                    }
                    com.focusr.v2.models.RuleType.SCHEDULED -> {
                        val fromTime = rule.fromTime ?: return@mapNotNull null
                        val fromMinutes = fromTime.first * 60 + fromTime.second
                        val minutesUntil = if (currentMinutes < fromMinutes) {
                            fromMinutes - currentMinutes
                        } else {
                            (1440 - currentMinutes) + fromMinutes // Next day
                        }
                        Pair(rule, minutesUntil)
                    }
                }
            }
            .minByOrNull { it.second }
        
        return upcomingRules?.let { (rule, minutes) ->
            val hours = minutes / 60
            val mins = minutes % 60
            val timeStr = when {
                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                hours > 0 -> "${hours}h"
                else -> "${mins}m"
            }
            if (rule.ruleType == com.focusr.v2.models.RuleType.SIMPLE) {
                "${rule.getDisplayName()} - $timeStr left"
            } else {
                "${rule.getDisplayName()} in $timeStr"
            }
        }
    }
}
