//package com.focusr.v2
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//
//data class MainUiState(
//    val advancedMode: Boolean = false,
//    val fromTime: Pair<Int, Int> = Pair(8, 0),
//    val toTime: Pair<Int, Int> = Pair(22, 0),
//    val blockedApps: Set<String> = emptySet(),
//    val blockingEnabled: Boolean = false
//)
//
//class MainViewModel(
//    private val preferencesManager: PreferencesManager
//) : ViewModel() {
//
//    private val _uiState = MutableStateFlow(MainUiState())
//    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
//
//    init {
//        // Collect all preferences and update UI state
//        viewModelScope.launch {
//            combine(
//                preferencesManager.advancedMode,
//                preferencesManager.fromTime,
//                preferencesManager.toTime,
//                preferencesManager.blockedApps,
//                preferencesManager.blockingEnabled
//            ) { advancedMode, fromTime, toTime, blockedApps, blockingEnabled ->
//                MainUiState(
//                    advancedMode = advancedMode,
//                    fromTime = fromTime,
//                    toTime = toTime,
//                    blockedApps = blockedApps,
//                    blockingEnabled = blockingEnabled
//                )
//            }.collect { newState ->
//                _uiState.value = newState
//            }
//        }
//    }
//
//    fun setAdvancedMode(enabled: Boolean) {
//        viewModelScope.launch {
//            preferencesManager.setAdvancedMode(enabled)
//        }
//    }
//
//    fun setFromTime(hour: Int, minute: Int) {
//        viewModelScope.launch {
//            preferencesManager.setFromTime(hour, minute)
//        }
//    }
//
//    fun setToTime(hour: Int, minute: Int) {
//        viewModelScope.launch {
//            preferencesManager.setToTime(hour, minute)
//        }
//    }
//
//    fun setBlockedApps(apps: Set<String>) {
//        viewModelScope.launch {
//            preferencesManager.setBlockedApps(apps)
//        }
//    }
//
//    fun setBlockingEnabled(enabled: Boolean) {
//        viewModelScope.launch {
//            preferencesManager.setBlockingEnabled(enabled)
//        }
//    }
//
//    fun setWasBlockingEnabledBeforeReboot(enabled: Boolean) {
//        viewModelScope.launch {
//            preferencesManager.setWasBlockingEnabledBeforeReboot(enabled)
//        }
//    }
//}


package com.focusr.v2
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusr.v2.data.TimerFeature
import com.focusr.v2.data.TimerSettings
import com.focusr.v2.ui.screens.Quadruple
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val advancedMode: Boolean = false,
    val fromTime: Pair<Int, Int> = Pair(8, 0),
    val toTime: Pair<Int, Int> = Pair(22, 0),
    val blockedApps: Set<String> = emptySet(),
    val blockingEnabled: Boolean = false,
    // Add these new fields
    val timerSettings: TimerSettings = TimerSettings(),
    val dailyUsageMinutes: Int = 0

)
{
    // Helper functions
    fun getEffectiveAdvancedMode(): Boolean {
        return when {
            timerSettings.enabledFeatures.contains(TimerFeature.ADVANCED_MODE) -> true
            timerSettings.enabledFeatures.contains(TimerFeature.SIMPLE_MODE) -> false
            else -> advancedMode
        }
    }

    fun hasFeature(feature: TimerFeature): Boolean {
        return timerSettings.enabledFeatures.contains(feature)
    }

    // ✅ Add this helper
    fun getActiveFeatures(): List<TimerFeature> {
        return timerSettings.enabledFeatures.toList()
    }
}

class MainViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val simpleModeDurationMinutes: StateFlow<Int> = preferencesManager
        .simpleModeDurationMinutes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = 60 // Default 1 hour
        )

    // MODIFY your existing init block to include the new flows
    init {
        // Collect all preferences and update UI state
        viewModelScope.launch {
            combine(
                combine(
                    preferencesManager.advancedMode,
                    preferencesManager.fromTime,
                    preferencesManager.toTime
                ) { advancedMode, fromTime, toTime ->
                    Triple(advancedMode, fromTime, toTime)
                },
                combine(
                    preferencesManager.blockedApps,
                    preferencesManager.blockingEnabled,
                    preferencesManager.timerSettings,
                    preferencesManager.dailyUsageMinutes
                ) { blockedApps, blockingEnabled, timerSettings, dailyUsage ->
                    Quadruple(blockedApps, blockingEnabled, timerSettings, dailyUsage)
                }
            ) { triple, quadruple ->
                MainUiState(
                    advancedMode = triple.first,
                    fromTime = triple.second,
                    toTime = triple.third,
                    blockedApps = quadruple.first,
                    blockingEnabled = quadruple.second,
                    timerSettings = quadruple.third,
                    dailyUsageMinutes = quadruple.fourth
                )
            }.collect { _uiState.value = it }
        }
    }


    fun setAdvancedMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAdvancedMode(enabled)
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

    fun setWasBlockingEnabledBeforeReboot(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setWasBlockingEnabledBeforeReboot(enabled)
        }
    }

    // ADD these functions to your existing MainViewModel class
    fun toggleFeature(feature: TimerFeature, enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.toggleFeature(feature, enabled)
        }
    }

    fun updateTimerSettings(settings: TimerSettings) {
        viewModelScope.launch {
            preferencesManager.updateTimerSettings(settings)
        }
    }

    fun hasFeature(feature: TimerFeature): Boolean {
        return _uiState.value.hasFeature(feature)
    }

    fun setSimpleModeDuration(minutes: Int) {
        viewModelScope.launch {
            preferencesManager.setSimpleModeDurationMinutes(minutes)
        }
    }

    // Add this function to your MainViewModel class

    fun autoToggleOffTimerFeature(feature: TimerFeature) {
        viewModelScope.launch {
            // Auto-toggle OFF the specific timer feature
            preferencesManager.toggleFeature(feature, false)

            // Optional: Log or show notification
            Log.d("MainViewModel", "${feature.displayName} auto-toggled OFF - timer expired")
        }
    }

}