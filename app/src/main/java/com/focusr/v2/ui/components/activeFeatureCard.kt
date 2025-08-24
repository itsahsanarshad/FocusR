package com.focusr.v2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import com.focusr.v2.MainUiState
import com.focusr.v2.data.TimerFeature
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Composable
fun ActiveFeaturesCard(
    uiState: MainUiState,
    onSettingsClick: () -> Unit,
    glassCard: Color,
    selectedGlassCard: Color,
    primaryAccent: Color,
    secondaryAccent: Color
) {
    val activeFeatures = uiState.getActiveFeatures()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                glassCard,
                RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = primaryAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Active Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeFeatures.isEmpty()) {
                // No active features
                EmptyFeaturesState(
                    onSettingsClick = onSettingsClick,
                    primaryAccent = primaryAccent
                )
            } else {
                // Show active features
                activeFeatures.forEach { feature ->
                    when (feature) {
                        TimerFeature.SIMPLE_MODE -> {
                            SimpleFeatureItem(
                                toTime = uiState.toTime,
                                primaryAccent = primaryAccent
                            )
                        }
                        TimerFeature.ADVANCED_MODE -> {
                            AdvancedFeatureItem(
                                fromTime = uiState.fromTime,
                                toTime = uiState.toTime,
                                primaryAccent = primaryAccent,
                                secondaryAccent = secondaryAccent
                            )
                        }
                        TimerFeature.DAILY_USAGE_LIMIT -> {
                            DailyLimitFeatureItem(
                                limitMinutes = uiState.timerSettings.dailyLimitMinutes,
                                primaryAccent = primaryAccent
                            )
                        }
                        TimerFeature.BREAK_REMINDERS -> {
                            BreakReminderFeatureItem(
                                intervalMinutes = uiState.timerSettings.breakIntervalMinutes,
                                primaryAccent = primaryAccent
                            )
                        }
//                        TimerFeature.SMART_SCHEDULING -> {
//                            SmartSchedulingFeatureItem(primaryAccent = primaryAccent)
//                        }
//                        TimerFeature.WEEKEND_MODE -> {
//                            WeekendModeFeatureItem(primaryAccent = primaryAccent)
//                        }
                    }

                    if (feature != activeFeatures.last()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeaturesState(
    onSettingsClick: () -> Unit,
    primaryAccent: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Outlined.Extension,
            contentDescription = null,
            tint = primaryAccent.copy(alpha = 0.6f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No features enabled",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = "Tap settings to enable features",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onSettingsClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = primaryAccent
            )
        ) {
            Text("Enable Features")
        }
    }
}

@Composable
private fun SimpleFeatureItem(
    toTime: Pair<Int, Int>,
    primaryAccent: Color
) {
    FeatureItemRow(
        icon = Icons.Outlined.Schedule,
        title = "Simple Mode",
        description = "Block until ${formatTime(toTime.first, toTime.second)}",
        color = primaryAccent
    )
}

@Composable
private fun AdvancedFeatureItem(
    fromTime: Pair<Int, Int>,
    toTime: Pair<Int, Int>,
    primaryAccent: Color,
    secondaryAccent: Color
) {
    Column {
        FeatureItemRow(
            icon = Icons.Outlined.Settings,
            title = "Advanced Mode",
            description = "Custom time range blocking",
            color = primaryAccent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FROM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = formatTime(fromTime.first, fromTime.second),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryAccent,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = formatTime(toTime.first, toTime.second),
                    style = MaterialTheme.typography.bodyMedium,
                    color = primaryAccent,
                    fontWeight = FontWeight.Medium
                )
            }
        }

    }
}

@Composable
private fun DailyLimitFeatureItem(
    limitMinutes: Int,
    primaryAccent: Color
) {
    FeatureItemRow(
        icon = Icons.Outlined.Shield,
        title = "Daily Limit",
        description = "Max usage: ${formatTimeTSS(limitMinutes)}",
        color = primaryAccent
    )
}

@Composable
private fun BreakReminderFeatureItem(
    intervalMinutes: Int,
    primaryAccent: Color
) {
    FeatureItemRow(
        icon = Icons.Outlined.Notifications,
        title = "Break Reminders",
        description = "Every $intervalMinutes minutes",
        color = primaryAccent
    )
}

@Composable
private fun SmartSchedulingFeatureItem(
    primaryAccent: Color
) {
    FeatureItemRow(
        icon = Icons.Outlined.Psychology,
        title = "Smart Scheduling",
        description = "AI-powered blocking times",
        color = primaryAccent
    )
}

@Composable
private fun WeekendModeFeatureItem(
    primaryAccent: Color
) {
    FeatureItemRow(
        icon = Icons.Outlined.Weekend,
        title = "Weekend Mode",
        description = "Relaxed settings on weekends",
        color = primaryAccent
    )
}

@Composable
private fun FeatureItemRow(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// Compact version for smaller spaces
@Composable
fun CompactActiveFeaturesCard(
    uiState: MainUiState,
    onSettingsClick: () -> Unit,
    glassCard: Color,
    primaryAccent: Color
) {
    val activeFeatures = uiState.getActiveFeatures()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(glassCard, RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = primaryAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${activeFeatures.size} features active",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    if (activeFeatures.isNotEmpty()) {
                        Text(
                            text = activeFeatures.take(2).joinToString(", ") { it.displayName },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Helper function to format time
private fun formatTimeTSS(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date())
}

fun formatTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
}

// Usage in your home screen:
/*
// Replace your ModernSettingsSection with:
item {
    ActiveFeaturesCard(
        uiState = uiState,
        onSettingsClick = {
            // Navigate to timer settings screen
            navController.navigate("timer_settings")
        },
        glassCard = glassCard,
        selectedGlassCard = selectedGlassCard,
        primaryAccent = primaryAccent,
        secondaryAccent = secondaryAccent
    )
}

// Or use the compact version:
item {
    CompactActiveFeaturesCard(
        uiState = uiState,
        onSettingsClick = {
            navController.navigate("timer_settings")
        },
        glassCard = glassCard,
        primaryAccent = primaryAccent
    )
}
*/