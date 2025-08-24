package com.focusr.v2.utils

// Helper function to format duration
fun formatDuration(totalMinutes: Int): String {
    return when {
        totalMinutes < 60 -> "${totalMinutes}m"
        totalMinutes % 60 == 0 -> "${totalMinutes / 60}h"
        else -> {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            "${hours}h ${minutes}m"
        }
    }
}