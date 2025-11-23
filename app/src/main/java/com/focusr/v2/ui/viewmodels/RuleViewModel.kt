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
    val isLoading: Boolean = false
)

class RuleViewModel(
    private val preferencesManager: PreferencesManager,
    private val blockingTimeManager: BlockingTimeManager,
    private val context: android.content.Context  // NEW
) : ViewModel() {

    private val _uiState = MutableStateFlow(RuleUiState())
    val uiState: StateFlow<RuleUiState> = _uiState.asStateFlow()

    init {
        // Collect rules and update UI state
        viewModelScope.launch {
            preferencesManager.blockingRules.collect { rules ->
                val activeCount = blockingTimeManager.getActiveRulesCount()
                val blockedCount = blockingTimeManager.getCurrentlyBlockedApps().size
                
                _uiState.value = RuleUiState(
                    allRules = rules,
                    activeRulesCount = activeCount,
                    blockedAppsCount = blockedCount,
                    isLoading = false
                )
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
     */
   fun toggleRuleEnabled(ruleId: String) {
    viewModelScope.launch {
        val rule = _uiState.value.allRules.find { it.id == ruleId }
        rule?.let {
            preferencesManager.updateRule(it.copy(enabled = !it.enabled))
            scheduleServiceIfNeeded()  // NEW
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
}
