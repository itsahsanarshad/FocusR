@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)

package com.focusr.v2.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.focusr.v2.AppInfo
import com.focusr.v2.AppManager
import com.focusr.v2.AppMonitoringService
//import com.focusr.v2.FocusBlockerAccessibilityService
import com.focusr.v2.MainActivity
import com.focusr.v2.MainViewModel
import com.focusr.v2.PermissionHelper
import com.focusr.v2.PreferencesManager
import com.focusr.v2.R
import com.focusr.v2.navigation.Screen
import com.focusr.v2.ui.components.ModernTimeButton
import com.focusr.v2.ui.components.ModernTimePickerDialog
import com.focusr.v2.ui.components.ModernTopBar
enum class PermissionStep {
    USAGE_STATS,
    OVERLAY,
    ACCESSIBILITY,
    COMPLETED
}

@Composable
fun HomeScreen(activity: ComponentActivity,navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember(context) { PreferencesManager(context.applicationContext) }
    val viewModel: MainViewModel = viewModel { MainViewModel(preferencesManager) }
    var onResumeCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    var showTimePicker by remember { mutableStateOf(false) }
    var isFromPicker by remember { mutableStateOf(true) }
    var showAppSelection by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var currentPermissionStep by remember { mutableStateOf(PermissionStep.COMPLETED) }
    var isWaitingForPermission by remember { mutableStateOf(false) }

    val fromTimeState = rememberTimePickerState(
        initialHour = uiState.fromTime.first,
        initialMinute = uiState.fromTime.second,
        is24Hour = false
    )
    val toTimeState = rememberTimePickerState(
        initialHour = uiState.toTime.first,
        initialMinute = uiState.toTime.second,
        is24Hour = false
    )

    val pulseAnimation by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

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

    // Monitoring Service Functions
    fun startMonitoringService() {
        try {
            val intent = Intent(context, AppMonitoringService::class.java)
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Log.e("HomeScreen", "Failed to start service", e)
        }
    }

    fun stopMonitoringService() {
        try {
            val intent = Intent(context, AppMonitoringService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            Log.e("HomeScreen", "Failed to stop service", e)
        }
    }

    fun checkPermissionFlow() {
        if (!isWaitingForPermission) return
        when (currentPermissionStep) {
            PermissionStep.USAGE_STATS -> {
                if (PermissionHelper.hasUsageStatsPermission(context)) {
                    currentPermissionStep = PermissionStep.OVERLAY
                    showPermissionDialog = true
                } else {
                    showPermissionDialog = true
                }
            }
            PermissionStep.OVERLAY -> {
                if (PermissionHelper.hasOverlayPermission(context)) {
                    currentPermissionStep = PermissionStep.ACCESSIBILITY
                    showPermissionDialog = true
                } else {
                    showPermissionDialog = true
                }
            }
            PermissionStep.ACCESSIBILITY -> {
                if (isAccessibilityServiceEnabled(context)) {
                    currentPermissionStep = PermissionStep.COMPLETED
                    showPermissionDialog = false
                    isWaitingForPermission = false
                    scope.launch {
                        viewModel.setBlockingEnabled(true)
                        delay(100)
                        startMonitoringService()
                    }
                } else {
                    showPermissionDialog = true
                }
            }
            PermissionStep.COMPLETED -> {
                showPermissionDialog = false
                isWaitingForPermission = false
            }
        }
    }

    // onResume checker
    LaunchedEffect(Unit) {
        if (activity is MainActivity) {
            activity.setOnResumeCallback {
                if (isWaitingForPermission) {
                    scope.launch {
                        delay(500)
                        checkPermissionFlow()
                    }
                }
            }
        }
    }

    // MAIN LAYOUT
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryGradient)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                ModernTopBar(primaryAccent)
            }
        ) { innerPadding ->

            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    ModernStatusCard(
                        isEnabled = uiState.blockingEnabled,
                        pulseScale = if (uiState.blockingEnabled) pulseAnimation else 1f,
                        onToggle = { enabled ->
                            if (enabled) {
                                val hasUsageStats = PermissionHelper.hasUsageStatsPermission(context)
                                val hasOverlay = PermissionHelper.hasOverlayPermission(context)
                                val hasAccessibility = isAccessibilityServiceEnabled(context)
                                val fromTime = uiState.fromTime
                                val toTime = uiState.toTime
                                val advancedMode = uiState.advancedMode

                                showTimeValidationMessage(context, fromTime, toTime, advancedMode)

                                when {
                                    !hasUsageStats -> {
                                        currentPermissionStep = PermissionStep.USAGE_STATS
                                        isWaitingForPermission = true
                                        showPermissionDialog = true
                                    }
                                    !hasOverlay -> {
                                        currentPermissionStep = PermissionStep.OVERLAY
                                        isWaitingForPermission = true
                                        showPermissionDialog = true
                                    }
                                    !hasAccessibility -> {
                                        currentPermissionStep = PermissionStep.ACCESSIBILITY
                                        isWaitingForPermission = true
                                        showPermissionDialog = true
                                    }
                                    else -> {
                                        scope.launch {
                                            viewModel.setBlockingEnabled(true)
                                            preferencesManager.setBlockingStartTime(System.currentTimeMillis())
                                            delay(100)
                                            startMonitoringService()
                                            Toast.makeText(context, "FocusR Service Started.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                scope.launch {
                                    viewModel.setBlockingEnabled(false)
                                    stopMonitoringService()
                                    Toast.makeText(context, "FocusR Service Stopped.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        glassCard = glassCard,
                        selectedGlassCard = selectedGlassCard,
                        primaryAccent = primaryAccent,
                        errorAccent = errorAccent
                    )
                }

                item {
                    ModernSettingsSection(
                        advancedMode = uiState.advancedMode,
                        fromTime = uiState.fromTime,
                        toTime = uiState.toTime,
                        onAdvancedModeToggle = { scope.launch { viewModel.setAdvancedMode(it) } },
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

                item {
                    ModernBlockedAppsSection(
                        blockedApps = uiState.blockedApps,
                        onAddApps = { showAppSelection = true },
                        onRemoveApp = { packageName ->
                            scope.launch {
                                val updatedApps = uiState.blockedApps.toMutableSet()
                                updatedApps.remove(packageName)
                                viewModel.setBlockedApps(updatedApps)
                            }
                        },
                        glassCard = glassCard,
                        errorAccent = errorAccent
                    )
                }
            }

            if (showTimePicker) {
                // Create local variables to store current time values
                val currentTime = remember {
                    val now = Calendar.getInstance()
                    Pair(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
                }

                var fromHour by remember { mutableStateOf(currentTime.first) }
                var fromMinute by remember { mutableStateOf(currentTime.second) }
                var toHour by remember { mutableStateOf(currentTime.first) }
                var toMinute by remember { mutableStateOf(currentTime.second) }
                ModernTimePickerDialog(
                    isFromPicker = isFromPicker,
                    initialHour = if (isFromPicker) fromHour else toHour,
                    initialMinute = if (isFromPicker) fromMinute else toMinute,
                    onDismiss = { showTimePicker = false },
                    onConfirm = { hour, minute ->
                        scope.launch {
                            if (isFromPicker) {
                                fromHour = hour
                                fromMinute = minute
                                viewModel.setFromTime(hour, minute)
                            } else {
                                toHour = hour
                                toMinute = minute
                                viewModel.setToTime(hour, minute)
                            }
                        }
                        showTimePicker = false
                    }
                )
            }

            if (showPermissionDialog) {
                ModernPermissionDialog(
                    step = currentPermissionStep,
                    onDismiss = {
                        showPermissionDialog = false
                        isWaitingForPermission = false
                        if (uiState.blockingEnabled) {
                            scope.launch { viewModel.setBlockingEnabled(false) }
                        }
                    },
                    onGrantPermission = {
                        when (currentPermissionStep) {
                            PermissionStep.USAGE_STATS -> {
                                PermissionHelper.requestUsageStatsPermission(activity)
                                showPermissionDialog = false
                            }
                            PermissionStep.OVERLAY -> {
                                PermissionHelper.requestOverlayPermission(activity)
                                showPermissionDialog = false
                            }
                            PermissionStep.ACCESSIBILITY -> {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                showPermissionDialog = false
                            }
                            PermissionStep.COMPLETED -> {
                                showPermissionDialog = false
                                isWaitingForPermission = false
                            }
                        }
                    }
                    //,
//                    glassCard = glassCard,
//                    primaryAccent = primaryAccent,
//                    errorAccent = errorAccent
                )
            }
        }
    }
}

@Composable
fun ModernStatusCard(
    isEnabled: Boolean,
    pulseScale: Float,
    onToggle: (Boolean) -> Unit,
    glassCard: Color,
    selectedGlassCard: Color,
    primaryAccent: Color,
    errorAccent: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale)
            .blur(radius = 0.dp)
            .background(
                if (isEnabled) selectedGlassCard else glassCard,
                RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Icon with Animation
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isEnabled) listOf(
                                primaryAccent.copy(alpha = 0.4f),
                                primaryAccent.copy(alpha = 0.1f)
                            ) else listOf(
                                errorAccent.copy(alpha = 0.4f),
                                errorAccent.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isEnabled) primaryAccent.copy(alpha = 0.6f)
                        else errorAccent.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isEnabled,
                    transitionSpec = {
                        slideInVertically() + fadeIn() with slideOutVertically() + fadeOut()
                    }
                ) { enabled ->
                    Icon(
                        imageVector = if (enabled) Icons.Filled.Shield else Icons.Filled.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = if (enabled) primaryAccent else errorAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEnabled) "Protection Active" else "Protection Disabled",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )

                Text(
                    text = if (isEnabled) "Your focus is protected from distractions"
                    else "Tap the switch below to enable protection",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Modern Toggle Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "OFF",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (!isEnabled) FontWeight.Bold else FontWeight.Normal,
                    color = if (!isEnabled) primaryAccent else Color.White.copy(alpha = 0.6f)
                )

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.scale(1.2f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = primaryAccent,
                        checkedTrackColor = primaryAccent.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Text(
                    text = "ON",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                    color = if (isEnabled) primaryAccent else Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
@Composable
fun ModernSettingsSection(
    advancedMode: Boolean,
    fromTime: Pair<Int, Int>,
    toTime: Pair<Int, Int>,
    onAdvancedModeToggle: (Boolean) -> Unit,
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
                if (advancedMode) selectedGlassCard else glassCard,
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
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        "Advanced Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Switch(
                    checked = advancedMode,
                    onCheckedChange = onAdvancedModeToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = primaryAccent,
                        checkedTrackColor = primaryAccent.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (advancedMode) {
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
            } else {
                ModernTimeButton(
                    icon = Icons.Outlined.Schedule,
                    label = "Block apps until (Select time onward till before Midnight)",
                    time = formatTime(toTime.first, toTime.second),
                    onClick = onToTimeClick,
                    color = primaryAccent
                )
            }
        }
    }
}

//@Composable
//fun ModernTimeButton(
//    icon: ImageVector,
//    label: String,
//    time: String,
//    onClick: () -> Unit,
//    color: Color,
//    glassCard: Color
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onClick() }
//            .drawBehind {
//                // Glassmorphism border effect
//                drawRoundRect(
//                    color = Color.White.copy(alpha = 0.2f),
//                    topLeft = Offset(0f, 0f),
//                    size = Size(size.width, size.height),
//                    cornerRadius = CornerRadius(32f, 32f),
//                    style = Stroke(width = 1.dp.toPx())
//                )
//            },
//        colors = CardDefaults.cardColors(
//            containerColor = glassCard
//        ),
//        shape = RoundedCornerShape(16.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(
//                    Brush.horizontalGradient(
//                        colors = listOf(
//                            Color.White.copy(alpha = 0.1f),
//                            Color.White.copy(alpha = 0.05f)
//                        )
//                    )
//                )
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.weight(1f)
//                ) {
//                    Icon(
//                        imageVector = icon,
//                        contentDescription = null,
//                        tint = color,
//                        modifier = Modifier.size(20.dp)
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = label,
//                        style = MaterialTheme.typography.bodyMedium,
//                        fontWeight = FontWeight.Medium,
//                        color = Color.White.copy(alpha = 0.9f)
//                    )
//                }
//
//                Text(
//                    text = time,
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = color
//                )
//            }
//        }
//    }
//}
@Composable
fun ModernBlockedAppsSection(
    blockedApps: Set<String>,
    onAddApps: () -> Unit,
    onRemoveApp: (String) -> Unit,
    glassCard: Color,
    errorAccent: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(glassCard, RoundedCornerShape(20.dp))
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
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = null,
                        tint = errorAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Blocked Apps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            "${blockedApps.size} apps selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (blockedApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(blockedApps.toList()) { packageName ->
                        ModernAppItem(
                            packageName = packageName,
                            onRemove = { onRemoveApp(packageName) },
                            errorAccent = errorAccent
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No apps selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernAppItem(
    packageName: String,
    onRemove: () -> Unit,
    errorAccent: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = Color.White
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove",
                    tint = errorAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ModernPermissionDialog(
    step: PermissionStep,
    onDismiss: () -> Unit,
    onGrantPermission: () -> Unit
) {
    val (title, description, buttonText, icon) = when (step) {
        PermissionStep.USAGE_STATS -> Quadruple(
            "Usage Access Permission",
            "Grant usage access to detect when blocked apps are opened and maintain your focus.",
            "Grant Permission",
            Icons.Outlined.Analytics
        )
        PermissionStep.OVERLAY -> Quadruple(
            "Display Over Apps",
            "Allow Focusr to display blocking screens over other apps to protect your focus.",
            "Grant Permission",
            Icons.Outlined.Layers
        )
        PermissionStep.ACCESSIBILITY -> Quadruple(
            "Accessibility Service",
            "Enable the accessibility service for instant app detection and seamless blocking.",
            "Enable Service",
            Icons.Outlined.Accessibility
        )
        PermissionStep.COMPLETED -> Quadruple(
            "Setup Complete!",
            "All permissions granted successfully. Your Focusr is now active.",
            "Got it",
            Icons.Outlined.CheckCircle
        )
    }

    // Enhanced Glassmorphism Design Tokens
    val primaryGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF1A1A2E),
            Color(0xFF16213E),
            Color(0xFF0F3460)
        ),
        radius = 800f
    )

    val cardBackgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.03f),
            Color.White.copy(alpha = 0.01f)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    val primaryAccent = Color(0xFF6C63FF)
    val secondaryAccent = Color(0xFF4ECDC4)
    val errorAccent = Color(0xFFFF6B6B)

    val glassButton = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.12f)
        ),
        start = Offset(0f, 0f),
        end = Offset(200f, 100f)
    )

    val glassButtonSecondary = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.04f)
        ),
        start = Offset(0f, 0f),
        end = Offset(200f, 100f)
    )

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = primaryGradient,
                    shape = RoundedCornerShape(24.dp)
                )
                .background(
                    brush = cardBackgroundGradient,
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Icon with Protection Status Style
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (step == PermissionStep.COMPLETED)
                                        secondaryAccent.copy(alpha = 0.3f)
                                    else
                                        errorAccent.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                radius = 60f
                            ),
                            shape = CircleShape
                        )
                        .background(
                            color = Color.White.copy(alpha = 0.04f),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = if (step == PermissionStep.COMPLETED)
                                secondaryAccent.copy(alpha = 0.6f)
                            else
                                errorAccent.copy(alpha = 0.6f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (step == PermissionStep.COMPLETED) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = secondaryAccent,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Block,
                            contentDescription = null,
                            tint = errorAccent,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Permission Status Title
                Text(
                    if (step == PermissionStep.COMPLETED) "Permission Enabled" else "Permission Disabled",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Permission Step Title
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.8f)
                )

                if (step != PermissionStep.COMPLETED) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Progress Indicator
                    val progress = when (step) {
                        PermissionStep.USAGE_STATS -> 0.33f
                        PermissionStep.OVERLAY -> 0.66f
                        PermissionStep.ACCESSIBILITY -> 1f
                        else -> 1f
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.04f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                primaryAccent,
                                                secondaryAccent
                                            )
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Step ${when(step) {
                                PermissionStep.USAGE_STATS -> "1"
                                PermissionStep.OVERLAY -> "2"
                                PermissionStep.ACCESSIBILITY -> "3"
                                else -> "3"
                            }} of 3",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Side-by-side Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (step == PermissionStep.COMPLETED)
                        Arrangement.Center
                    else
                        Arrangement.spacedBy(12.dp)
                ) {
                    if (step != PermissionStep.COMPLETED) {
                        // Cancel Button
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .background(
                                    brush = glassButtonSecondary,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Text(
                                "Cancel",
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Primary Action Button
                    Button(
                        onClick = onGrantPermission,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .background(
                                brush = glassButton,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Enable",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium

                            )
                            if (step != PermissionStep.COMPLETED) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Outlined.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}






// Helper data class for the permission dialog
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)



fun isAccessibilityServiceEnabled(context: Context): Boolean {
//    val expectedComponentName = ComponentName(context, FocusBlockerAccessibilityService::class.java)
//    val enabledServicesSetting = Settings.Secure.getString(
//        context.contentResolver,
//        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
//    ) ?: return false
//
//    return enabledServicesSetting.split(":").any {
//        ComponentName.unflattenFromString(it) == expectedComponentName
//    }

    return true
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


fun showTimeValidationMessage(
    context: Context,
    fromTime: Pair<Int, Int>,
    toTime: Pair<Int, Int>,
    isAdvancedMode: Boolean
) {
    val now = Calendar.getInstance()
    val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val fromMinutes = fromTime.first * 60 + fromTime.second
    val toMinutes = toTime.first * 60 + toTime.second

    fun formatTime(pair: Pair<Int, Int>): String {
        val hour = pair.first
        val minute = pair.second
        val amPm = if (hour >= 12) "PM" else "AM"
        val formattedHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "${formattedHour}:${minute.toString().padStart(2, '0')} $amPm"
    }

    if (!isAdvancedMode) {
        // 🔹 Simple Mode
        if (toMinutes <= currentMinutes) {
            Toast.makeText(context, "Please select a future time", Toast.LENGTH_SHORT).show()
        } else if (toMinutes > (23 * 60 + 59)) {
            Toast.makeText(context, "Simple mode only supports blocking before midnight", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Blocking will end at ${formatTime(toTime)}", Toast.LENGTH_SHORT).show()
        }
    } else {
        // 🔸 Advanced Mode
        if (fromMinutes > toMinutes) {
            // Overnight session
            if (fromMinutes >= currentMinutes) {
                Toast.makeText(
                    context,
                    "Session will start today at ${formatTime(fromTime)} and end tomorrow at ${formatTime(toTime)}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Session has started and will end tomorrow at ${formatTime(toTime)}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            // Same-day session
            if (fromMinutes >= currentMinutes) {
                Toast.makeText(
                    context,
                    "Session will start today at ${formatTime(fromTime)} and end at ${formatTime(toTime)}",
                    Toast.LENGTH_LONG
                ).show()
            } else if (currentMinutes in fromMinutes..toMinutes) {
                Toast.makeText(
                    context,
                    "Blocking session is currently active and will end at ${formatTime(toTime)}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Session has already ended for today.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}