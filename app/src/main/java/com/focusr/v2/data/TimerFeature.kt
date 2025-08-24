package com.focusr.v2.data

// New feature system - add this to your project
enum class TimerFeature(val displayName: String, val description: String) {
    SIMPLE_MODE("Simple Mode", "Basic timer with end time only"),
    ADVANCED_MODE("Advanced Mode", "Full scheduling with start and end times"),
    DAILY_USAGE_LIMIT("Daily Usage Limit", "Set maximum daily usage time"),
    BREAK_REMINDERS("Break Reminders", "Get notified to take breaks"),
//    SMART_SCHEDULING("Smart Scheduling", "AI-powered optimal blocking times"),
//    WEEKEND_MODE("Weekend Mode", "Different settings for weekends")
}
