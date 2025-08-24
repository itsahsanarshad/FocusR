package com.focusr.v2.data

// New settings data class
data class TimerSettings(
    val enabledFeatures: Set<TimerFeature> = setOf(TimerFeature.SIMPLE_MODE),
    val dailyLimitMinutes: Int = 480, // 8 hours default
    val breakIntervalMinutes: Int = 25, // Pomodoro-style
//    val weekendEnabled: Boolean = true,
//    val smartSchedulingEnabled: Boolean = false
)