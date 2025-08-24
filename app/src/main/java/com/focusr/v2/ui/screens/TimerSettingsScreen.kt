package com.focusr.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.focusr.v2.data.TimerFeature
import com.focusr.v2.ui.components.ModernTimeButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.focusr.v2.ui.components.DurationPickerDialog
import com.focusr.v2.ui.components.DurationPresetChip
import com.focusr.v2.ui.components.ModernTimePickerDialog
import com.focusr.v2.utils.formatDuration
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Time picker state
    var showTimePicker by remember { mutableStateOf(false) }
    var isFromPicker by remember { mutableStateOf(true) }

    // State variables
    var showDurationPicker by remember { mutableStateOf(false) }
    val simpleModeDurationMinutes by viewModel.simpleModeDurationMinutes.collectAsState()

    // Glassmorphism Design Tokens
    val primaryGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)),
        center = Offset(0.5f, 0.3f),
        radius = 1200f
    )
    val glassCard = Color.White.copy(alpha = 0.15f)
    val selectedGlassCard = Color.White.copy(alpha = 0.25f)
    val primaryAccent = Color(0xFF6C63FF)
    val secondaryAccent = Color(0xFF4ECDC4)
    val errorAccent = Color(0xFFFF6B6B)
    val serviceManager = ServiceManager(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Timer Settings",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Choose Your Features",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tap cards to activate features",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Feature Cards with embedded settings
            items(getFeatureList()) { feature ->
                FeatureCard(
                    feature = feature,
                    isEnabled = uiState.hasFeature(feature.type),
                    onToggle = { enabled ->
                        viewModel.toggleFeature(feature.type, enabled)
                    },
                    settingsContent = when (feature.type) {
                        TimerFeature.SIMPLE_MODE -> {
                            {
                                SimpleModeTiming(
                                    durationMinutes = simpleModeDurationMinutes,
                                    onDurationClick = {
                                        showDurationPicker = true
                                    },
                                    onPresetClick = { minutes ->
                                        viewModel.setSimpleModeDuration(minutes)
                                    },
                                    glassCard = glassCard,
                                    selectedGlassCard = selectedGlassCard,
                                    primaryAccent = primaryAccent,
                                    secondaryAccent = secondaryAccent
                                )
                            }
                        }
                        TimerFeature.ADVANCED_MODE -> {
                            {
                                AdvancedModeTiming(
                                    fromTime = uiState.fromTime,
                                    toTime = uiState.toTime,
                                    onFromTimeClick = {
                                        isFromPicker = true
                                        showTimePicker = true
                                    },
                                    onToTimeClick = {
                                        isFromPicker = false
                                        showTimePicker = true
                                    },
                                    glassCard = glassCard,
                                    selectedGlassCard = selectedGlassCard,
                                    primaryAccent = primaryAccent,
                                    secondaryAccent = secondaryAccent
                                )
                            }
                        }
                        TimerFeature.DAILY_USAGE_LIMIT -> {
                            {
                                DailyLimitCard(
                                    currentLimit = uiState.timerSettings.dailyLimitMinutes,
                                    onLimitChange = { newLimit ->
                                        val newSettings = uiState.timerSettings.copy(dailyLimitMinutes = newLimit)
                                        viewModel.updateTimerSettings(newSettings)
                                    }
                                )
                            }
                        }
                        TimerFeature.BREAK_REMINDERS -> {
                            {
                                BreakReminderCard(
                                    currentInterval = uiState.timerSettings.breakIntervalMinutes,
                                    onIntervalChange = { newInterval ->
                                        val newSettings = uiState.timerSettings.copy(breakIntervalMinutes = newInterval)
                                        viewModel.updateTimerSettings(newSettings)
                                    }
                                )
                            }
                        }
                        else -> null
                    }
                )
            }
        }
    }

    // Duration picker dialog
    if (showDurationPicker) {
        DurationPickerDialog(
            currentDuration = simpleModeDurationMinutes,
            onDurationSelected = { duration ->
                viewModel.setSimpleModeDuration(duration)
            },
            onDismiss = { showDurationPicker = false }
        )
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val currentTime = remember {
            val now = Calendar.getInstance()
            Pair(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        }

        var fromHour by remember { mutableStateOf(uiState.fromTime.first) }
        var fromMinute by remember { mutableStateOf(uiState.fromTime.second) }
        var toHour by remember { mutableStateOf(uiState.toTime.first) }
        var toMinute by remember { mutableStateOf(uiState.toTime.second) }

        ModernTimePickerDialog(
            isFromPicker = isFromPicker,
            initialHour = if (isFromPicker) fromHour else toHour,
            initialMinute = if (isFromPicker) fromMinute else toMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                scope.launch {
                    val wasRunning = uiState.blockingEnabled

                    // Always stop and reset service if it was running
                    if (wasRunning) {
                        viewModel.setBlockingEnabled(false)
                        // serviceManager.toggleBlocking(false) // Uncomment if you have serviceManager
                    }

                    // Update the time variables and viewModel
                    if (isFromPicker) {
                        fromHour = hour
                        fromMinute = minute
                        viewModel.setFromTime(hour, minute)
                    } else {
                        toHour = hour
                        toMinute = minute
                        viewModel.setToTime(hour, minute)
                    }

                    // Show appropriate message if needed
                    // You can add Toast message here if needed
                }
                showTimePicker = false
            }
        )
    }
}

//@Composable
//fun TimerSettingsScreen(
//    viewModel: MainViewModel,
//    onNavigateBack: () -> Unit
//) {
//    val uiState by viewModel.uiState.collectAsState()
//    val context = LocalContext.current
//    val scope = rememberCoroutineScope()
//
//    // Time picker state
//    var showTimePicker by remember { mutableStateOf(false) }
//    var isFromPicker by remember { mutableStateOf(true) }
//
//    // State variables
//    var showDurationPicker by remember { mutableStateOf(false) }
//    val simpleModeDurationMinutes by viewModel.simpleModeDurationMinutes.collectAsState()
//
//    // Glassmorphism Design Tokens
//    val primaryGradient = Brush.radialGradient(
//        colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)),
//        center = Offset(0.5f, 0.3f),
//        radius = 1200f
//    )
//    val glassCard = Color.White.copy(alpha = 0.15f)
//    val selectedGlassCard = Color.White.copy(alpha = 0.25f)
//    val primaryAccent = Color(0xFF6C63FF)
//    val secondaryAccent = Color(0xFF4ECDC4)
//    val errorAccent = Color(0xFFFF6B6B)
//    val serviceManager = ServiceManager(context)
//
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(MaterialTheme.colorScheme.background)
//    ) {
//        // Top App Bar
//        TopAppBar(
//            title = {
//                Text(
//                    text = "Timer Settings",
//                    style = MaterialTheme.typography.headlineSmall
//                )
//            },
//            navigationIcon = {
//                IconButton(onClick = onNavigateBack) {
//                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                }
//            },
//            colors = TopAppBarDefaults.topAppBarColors(
//                containerColor = MaterialTheme.colorScheme.surface
//            )
//        )
//
//        LazyColumn(
//            modifier = Modifier.fillMaxSize(),
//            contentPadding = PaddingValues(16.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            item {
//                Text(
//                    text = "Choose Your Features",
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.onBackground
//                )
//                Text(
//                    text = "Enable the features you want to use",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//                Spacer(modifier = Modifier.height(8.dp))
//            }
//
//            // Feature Cards
//            items(getFeatureList()) { feature ->
//                FeatureCard(
//                    feature = feature,
//                    isEnabled = uiState.hasFeature(feature.type),
//                    onToggle = { enabled ->
//                        viewModel.toggleFeature(feature.type, enabled)
//                    }
//                )
//            }
//
//            // Simple Mode Time Settings
//            if (uiState.hasFeature(TimerFeature.SIMPLE_MODE)) {
//                item {
//                    // Replace your old SimpleModeTiming call with:
//                    SimpleModeTiming(
//                        durationMinutes = simpleModeDurationMinutes,
//                        onDurationClick = {
//                            showDurationPicker = true
//                        },
//                        onPresetClick = { minutes ->
//                            viewModel.setSimpleModeDuration(minutes)
//                        },
//                        glassCard = glassCard,
//                        selectedGlassCard = selectedGlassCard,
//                        primaryAccent = primaryAccent,
//                        secondaryAccent = secondaryAccent
//                    )
//                }
//            }
//
//            // Advanced Mode Time Settings
//            if (uiState.hasFeature(TimerFeature.ADVANCED_MODE)) {
//                item {
//                    AdvancedModeTiming(
//                        fromTime = uiState.fromTime,
//                        toTime = uiState.toTime,
//                        onFromTimeClick = {
//                            isFromPicker = true
//                            showTimePicker = true
//                        },
//                        onToTimeClick = {
//                            isFromPicker = false
//                            showTimePicker = true
//                        },
//                        glassCard = glassCard,
//                        selectedGlassCard = selectedGlassCard,
//                        primaryAccent = primaryAccent,
//                        secondaryAccent = secondaryAccent
//                    )
//                }
//            }
//
//            // Daily Limit Settings
//            if (uiState.hasFeature(TimerFeature.DAILY_USAGE_LIMIT)) {
//                item {
//                    DailyLimitCard(
//                        currentLimit = uiState.timerSettings.dailyLimitMinutes,
//                        onLimitChange = { newLimit ->
//                            val newSettings = uiState.timerSettings.copy(dailyLimitMinutes = newLimit)
//                            viewModel.updateTimerSettings(newSettings)
//                        }
//                    )
//                }
//            }
//
//            // Break Reminder Settings
//            if (uiState.hasFeature(TimerFeature.BREAK_REMINDERS)) {
//                item {
//                    BreakReminderCard(
//                        currentInterval = uiState.timerSettings.breakIntervalMinutes,
//                        onIntervalChange = { newInterval ->
//                            val newSettings = uiState.timerSettings.copy(breakIntervalMinutes = newInterval)
//                            viewModel.updateTimerSettings(newSettings)
//                        }
//                    )
//                }
//            }
//
//        }
//    }
//
//    // Duration picker dialog
//    if (showDurationPicker) {
//        DurationPickerDialog(
//            currentDuration = simpleModeDurationMinutes,
//            onDurationSelected = { duration ->
//                viewModel.setSimpleModeDuration(duration)
//            },
//            onDismiss = { showDurationPicker = false }
//        )
//    }
//
//    // Time Picker Dialog
//    if (showTimePicker) {
//        val currentTime = remember {
//            val now = Calendar.getInstance()
//            Pair(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
//        }
//
//        var fromHour by remember { mutableStateOf(uiState.fromTime.first) }
//        var fromMinute by remember { mutableStateOf(uiState.fromTime.second) }
//        var toHour by remember { mutableStateOf(uiState.toTime.first) }
//        var toMinute by remember { mutableStateOf(uiState.toTime.second) }
//
//        ModernTimePickerDialog(
//            isFromPicker = isFromPicker,
//            initialHour = if (isFromPicker) fromHour else toHour,
//            initialMinute = if (isFromPicker) fromMinute else toMinute,
//            onDismiss = { showTimePicker = false },
//            onConfirm = { hour, minute ->
//                scope.launch {
//                    val wasRunning = uiState.blockingEnabled
//
//                    // Always stop and reset service if it was running
//                    if (wasRunning) {
//                        viewModel.setBlockingEnabled(false)
//                        // serviceManager.toggleBlocking(false) // Uncomment if you have serviceManager
//                    }
//
//                    // Update the time variables and viewModel
//                    if (isFromPicker) {
//                        fromHour = hour
//                        fromMinute = minute
//                        viewModel.setFromTime(hour, minute)
//                    } else {
//                        toHour = hour
//                        toMinute = minute
//                        viewModel.setToTime(hour, minute)
//                    }
//
//                    // Show appropriate message if needed
//                    // You can add Toast message here if needed
//                }
//                showTimePicker = false
//            }
//        )
//    }
//}
@Composable
private fun SimpleModeTiming(
    durationMinutes: Int,
    onDurationClick: () -> Unit,
    onPresetClick: (Int) -> Unit,
    glassCard: Color,
    selectedGlassCard: Color,
    primaryAccent: Color,
    secondaryAccent: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.Black.copy(0.2f),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = primaryAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Timer Duration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick preset buttons
            Text(
                text = "Quick Select",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val presets = listOf(15, 30, 60, 90, 120, 180, 240) // 15min to 4hours
                items(presets) { preset ->
                    DurationPresetChip(
                        minutes = preset,
                        isSelected = durationMinutes == preset,
                        onClick = { onPresetClick(preset) },
                        color = primaryAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom duration button
            ModernDurationButton(
                icon = Icons.Outlined.Timer,
                label = "Custom Duration",
                duration = formatDuration(durationMinutes),
                onClick = onDurationClick,
                color = primaryAccent
            )
        }
    }
}
@Composable
private fun AdvancedModeTiming(
    fromTime: Pair<Int, Int>,
    toTime: Pair<Int, Int>,
    onFromTimeClick: () -> Unit,
    onToTimeClick: () -> Unit,
    glassCard: Color,
    selectedGlassCard: Color,
    primaryAccent: Color,
    secondaryAccent: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.Black.copy(0.2f),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = primaryAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Advanced Mode Timing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ModernTimeButton(
                icon = Icons.Outlined.PlayArrow,
                label = "Block FROM",
                time = formatTime(fromTime.first, fromTime.second),
                onClick = onFromTimeClick,
                color = secondaryAccent
            )

            Spacer(modifier = Modifier.height(12.dp))

            ModernTimeButton(
                icon = Icons.Outlined.Stop,
                label = "Block TO",
                time = formatTime(toTime.first, toTime.second),
                onClick = onToTimeClick,
                color = primaryAccent
            )
        }
    }
}

//@Composable
//private fun FeatureCard(
//    feature: FeatureInfo,
//    isEnabled: Boolean,
//    onToggle: (Boolean) -> Unit
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = if (isEnabled)
//                MaterialTheme.colorScheme.primaryContainer
//            else
//                MaterialTheme.colorScheme.surface
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .clickable { onToggle(!isEnabled) }
//                .padding(16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.weight(1f)
//            ) {
//                Icon(
//                    imageVector = feature.icon,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .size(24.dp)
//                        .clip(RoundedCornerShape(6.dp)),
//                    tint = if (isEnabled)
//                        MaterialTheme.colorScheme.primary
//                    else
//                        MaterialTheme.colorScheme.onSurfaceVariant
//                )
//                Spacer(modifier = Modifier.width(12.dp))
//                Column {
//                    Text(
//                        text = feature.type.displayName,
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Medium,
//                        color = if (isEnabled)
//                            MaterialTheme.colorScheme.onPrimaryContainer
//                        else
//                            MaterialTheme.colorScheme.onSurface
//                    )
//                    Text(
//                        text = feature.type.description,
//                        style = MaterialTheme.typography.bodySmall,
//                        color = if (isEnabled)
//                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
//                        else
//                            MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
//
//            Switch(
//                checked = isEnabled,
//                onCheckedChange = onToggle
//            )
//        }
//    }
//}

@Composable
private fun FeatureCard(
    feature: FeatureInfo,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    settingsContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Main card content (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(!isEnabled) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        tint = if (isEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = feature.type.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isEnabled)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = feature.type.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle
                    )

                    // Expand/collapse icon (only show if there are settings)
                    if (settingsContent != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isEnabled) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isEnabled) "Collapse" else "Expand",
                            tint = if (isEnabled)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Settings content (shown when enabled and settings exist)
            if (settingsContent != null) {
                AnimatedVisibility(
                    visible = isEnabled,
                    enter = expandVertically(
                        animationSpec = tween(300),
                        expandFrom = Alignment.Top
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = shrinkVertically(
                        animationSpec = tween(300),
                        shrinkTowards = Alignment.Top
                    ) + fadeOut(animationSpec = tween(300))
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        )
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = if (isEnabled)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        settingsContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyLimitCard(
    currentLimit: Int,
    onLimitChange: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentLimit.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(0.2f),
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Daily Usage Limit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Maximum daily usage: ${formatTimeTSS(sliderValue.toInt())}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onLimitChange(sliderValue.toInt()) },
                valueRange = 15f..240f, // 15 minutes to 240 minutes (4 hours)
                steps = 14, // 15-minute increments: (240-15)/15 = 15 steps, so steps = 14
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.surfaceTint,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "15m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "12h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun BreakReminderCard(
    currentInterval: Int,
    onIntervalChange: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentInterval.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(0.2f),
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Break Reminders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Break interval: ${sliderValue.toInt()} minutes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onIntervalChange(sliderValue.toInt()) },
                valueRange = 15f..60f,
                steps = 8, // 5-minute increments
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.surfaceTint,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "15min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "60min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}



// ModernDurationButton component
@Composable
private fun ModernDurationButton(
    icon: ImageVector,
    label: String,
    duration: String,
    onClick: () -> Unit,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.1f),
                        color.copy(alpha = 0.05f)
                    )
                )
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = duration,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }

        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "Edit duration",
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

// Custom Duration Picker Dialog



//@Composable
//fun DurationPickerDialog(
//    currentDuration: Int,
//    onDurationSelected: (Int) -> Unit,
//    onDismiss: () -> Unit
//) {
//    val hours = remember { mutableIntStateOf(currentDuration / 60) }
//    val minutes = remember { mutableIntStateOf(currentDuration % 60) }
//
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = {
//            Text(
//                "Custom Duration",
//                style = MaterialTheme.typography.headlineSmall,
//                fontWeight = FontWeight.Bold
//            )
//        },
//        text = {
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    "Set your custom blocking duration",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
//                    modifier = Modifier.padding(bottom = 16.dp)
//                )
//
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(16.dp)
//                ) {
//                    // Hours picker
//                    Column(
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Text(
//                            "Hours",
//                            style = MaterialTheme.typography.labelMedium,
//                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//
//                        Row(
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            IconButton(
//                                onClick = { if (hours.intValue > 0) hours.intValue-- }
//                            ) {
//                                Icon(Icons.Default.Remove, contentDescription = "Decrease hours")
//                            }
//
//                            Text(
//                                text = hours.intValue.toString(),
//                                style = MaterialTheme.typography.headlineMedium,
//                                fontWeight = FontWeight.Bold,
//                                modifier = Modifier.width(48.dp),
//                                textAlign = TextAlign.Center
//                            )
//
//                            IconButton(
//                                onClick = { if (hours.intValue < 12) hours.intValue++ }
//                            ) {
//                                Icon(Icons.Default.Add, contentDescription = "Increase hours")
//                            }
//                        }
//                    }
//
//                    Text(
//                        ":",
//                        style = MaterialTheme.typography.headlineMedium,
//                        fontWeight = FontWeight.Bold
//                    )
//
//                    // Minutes picker
//                    Column(
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Text(
//                            "Minutes",
//                            style = MaterialTheme.typography.labelMedium,
//                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//
//                        Row(
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            IconButton(
//                                onClick = {
//                                    if (minutes.intValue >= 5) minutes.intValue -= 5
//                                    else minutes.intValue = 0
//                                }
//                            ) {
//                                Icon(Icons.Default.Remove, contentDescription = "Decrease minutes")
//                            }
//
//                            Text(
//                                text = minutes.intValue.toString(),
//                                style = MaterialTheme.typography.headlineMedium,
//                                fontWeight = FontWeight.Bold,
//                                modifier = Modifier.width(48.dp),
//                                textAlign = TextAlign.Center
//                            )
//
//                            IconButton(
//                                onClick = {
//                                    if (minutes.intValue < 45) minutes.intValue += 15
//                                    else minutes.intValue = 45
//                                }
//                            ) {
//                                Icon(Icons.Default.Add, contentDescription = "Increase minutes")
//                            }
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Text(
//                    "Total: ${formatDuration(hours.intValue * 60 + minutes.intValue)}",
//                    style = MaterialTheme.typography.bodyLarge,
//                    fontWeight = FontWeight.Medium,
//                    color = MaterialTheme.colorScheme.primary
//                )
//            }
//        },
//        confirmButton = {
//            TextButton(
//                onClick = {
//                    val totalMinutes = hours.intValue * 60 + minutes.intValue
//                    if (totalMinutes > 0) {
//                        onDurationSelected(totalMinutes)
//                        onDismiss()
//                    }
//                }
//            ) {
//                Text("Set Duration")
//            }
//        },
//        dismissButton = {
//            TextButton(onClick = onDismiss) {
//                Text("Cancel")
//            }
//        }
//    )
//}

// Helper data class
data class FeatureInfo(
    val type: TimerFeature,
    val icon: ImageVector
)

// Helper function to get feature list with icons
private fun getFeatureList(): List<FeatureInfo> = listOf(
    FeatureInfo(TimerFeature.SIMPLE_MODE, Icons.Default.Schedule),
    FeatureInfo(TimerFeature.ADVANCED_MODE, Icons.Default.Settings),
    FeatureInfo(TimerFeature.DAILY_USAGE_LIMIT, Icons.Default.Shield),
    FeatureInfo(TimerFeature.BREAK_REMINDERS, Icons.Default.Notifications),
//    FeatureInfo(TimerFeature.SMART_SCHEDULING, Icons.Default.Psychology),
//    FeatureInfo(TimerFeature.WEEKEND_MODE, Icons.Default.Weekend)
)

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

