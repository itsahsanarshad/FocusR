package com.focusr.v2
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val advancedMode: Boolean = false,
    val fromTime: Pair<Int, Int> = Pair(8, 0),
    val toTime: Pair<Int, Int> = Pair(22, 0),
    val blockedApps: Set<String> = emptySet(),
    val blockingEnabled: Boolean = false
)

class MainViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Collect all preferences and update UI state
        viewModelScope.launch {
            combine(
                preferencesManager.advancedMode,
                preferencesManager.fromTime,
                preferencesManager.toTime,
                preferencesManager.blockedApps,
                preferencesManager.blockingEnabled
            ) { advancedMode, fromTime, toTime, blockedApps, blockingEnabled ->
                MainUiState(
                    advancedMode = advancedMode,
                    fromTime = fromTime,
                    toTime = toTime,
                    blockedApps = blockedApps,
                    blockingEnabled = blockingEnabled
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setAdvancedMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAdvancedMode(false)
        }
    }

    fun setFromTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesManager.setFromTime(hour, minute)
        }
    }

    fun setToTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesManager.setToTime(hour, minute)
        }
    }

    fun setBlockedApps(apps: Set<String>) {
        viewModelScope.launch {
            preferencesManager.setBlockedApps(apps)
        }
    }

    fun setBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBlockingEnabled(enabled)
        }
    }
}