@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)

package com.focusr.v2

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.focusr.v2.ui.theme.OpalForAndroidTheme
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource

import java.time.LocalTime

enum class PermissionStep {
    USAGE_STATS,
    OVERLAY,
    ACCESSIBILITY,
    COMPLETED
}

class MainActivity : ComponentActivity() {
    private var onResumeCallback: (() -> Unit)? = null

    private lateinit var preferencesManager: PreferencesManager

    fun setOnResumeCallback(callback: () -> Unit) {
        onResumeCallback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = PreferencesManager(applicationContext)
        enableEdgeToEdge()

        setContent {
            OpalForAndroidTheme(dynamicColor = false,darkTheme = false,) {
                val viewModel: MainViewModel = viewModel { MainViewModel(PreferencesManager(this@MainActivity)) }

                // Modern gradient background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.White,
                        topBar = {
                            ModernTopBar()
                        }
                    ) { innerPadding ->
                        MainScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel,
                            activity = this@MainActivity
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        onResumeCallback?.invoke()
    }
}

@Composable
fun ModernTopBar() {
    var timeString by remember { mutableStateOf(getCurrentTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            timeString = getCurrentTime()
            delay(1000)
        }
    }

    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    Image(
                        painter = painterResource(id = R.drawable.self_improvement),
                        contentDescription = "FocusR Logo",
                        modifier = Modifier
                            .height(48.dp)
                            .padding(vertical = 4.dp)
                    )
                    Text(
                        "FocusR",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D9CDB),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(vertical = 4.dp)

                    )
                }

                Text(
                    "Current Time $timeString",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    activity: ComponentActivity
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val preferencesManager = remember(context) { PreferencesManager(context.applicationContext) }

    var showTimePicker by remember { mutableStateOf(false) }
    var isFromPicker by remember { mutableStateOf(true) }
    var showAppSelection by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var currentPermissionStep by remember { mutableStateOf(PermissionStep.COMPLETED) }
    var isWaitingForPermission by remember { mutableStateOf(false) }
    var simpleTimerSelected by remember { mutableStateOf("30m") }
    var customValue by remember { mutableStateOf("") }
    var isHour by remember { mutableStateOf(false) }

    // Animated values for modern effects
    val pulseAnimation by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Function to start the service properly
    fun startMonitoringService() {
        Log.d("MainActivity", "Starting monitoring service...")
        try {
            val intent = Intent(context, AppMonitoringService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d("MainActivity", "Starting foreground service for API ${Build.VERSION.SDK_INT}")
                context.startForegroundService(intent)
            } else {
                Log.d("MainActivity", "Starting regular service for API ${Build.VERSION.SDK_INT}")
                context.startService(intent)
            }

            Log.d("MainActivity", "Service start command sent successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start service", e)
        }
    }

    // Function to stop the service
    fun stopMonitoringService() {
        Log.d("MainActivity", "Stopping monitoring service...")
        try {
            val intent = Intent(context, AppMonitoringService::class.java)
            context.stopService(intent)
            Log.d("MainActivity", "Service stop command sent successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to stop service", e)
        }
    }

    // Function to check and update permission flow
    fun checkPermissionFlow() {
        if (!isWaitingForPermission) return

        Log.d("MainActivity", "Checking permission flow for step: $currentPermissionStep")

        when (currentPermissionStep) {
            PermissionStep.USAGE_STATS -> {
                if (PermissionHelper.hasUsageStatsPermission(context)) {
                    Log.d("MainActivity", "Usage stats permission granted, moving to overlay")
                    currentPermissionStep = PermissionStep.OVERLAY
                    showPermissionDialog = true
                } else {
                    Log.d("MainActivity", "Usage stats permission still not granted")
                    showPermissionDialog = true
                }
            }
            PermissionStep.OVERLAY -> {
                if (PermissionHelper.hasOverlayPermission(context)) {
                    Log.d("MainActivity", "Overlay permission granted, moving to accessibility")
                    currentPermissionStep = PermissionStep.ACCESSIBILITY
                    showPermissionDialog = true
                } else {
                    Log.d("MainActivity", "Overlay permission still not granted")
                    showPermissionDialog = true
                }
            }
            PermissionStep.ACCESSIBILITY -> {
                if (isAccessibilityServiceEnabled(context)) {
                    Log.d("MainActivity", "All permissions granted, starting service")
                    currentPermissionStep = PermissionStep.COMPLETED
                    showPermissionDialog = false
                    isWaitingForPermission = false

                    // All permissions granted, enable blocking and start service
                    scope.launch {
                        viewModel.setBlockingEnabled(true)
                        // Small delay to ensure state is updated
                        delay(100)
                        startMonitoringService()
                    }
                } else {
                    Log.d("MainActivity", "Accessibility service still not enabled")
                    showPermissionDialog = true
                }
            }
            PermissionStep.COMPLETED -> {
                showPermissionDialog = false
                isWaitingForPermission = false
            }
        }
    }

    // Set up the onResume callback
    LaunchedEffect(Unit) {
        (activity as? MainActivity)?.setOnResumeCallback {
            if (isWaitingForPermission) {
                scope.launch {
                    delay(500)
                    checkPermissionFlow()
                }
            }
        }
    }

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            // Modern Hero Section with Status
            ModernStatusCard(
                isEnabled = uiState.blockingEnabled,
                pulseScale = if (uiState.blockingEnabled) pulseAnimation else 1f,
                onToggle = { enabled ->
                    Log.d("MainActivity", "Blocking switch toggled to: $enabled")


                    if (enabled) {

                        // Check all permissions and start the flow
                        val hasUsageStats = PermissionHelper.hasUsageStatsPermission(context)
                        val hasOverlay = PermissionHelper.hasOverlayPermission(context)
                        val hasAccessibility = isAccessibilityServiceEnabled(context)


                        val fromTime = uiState.fromTime
                        val toTime = uiState.toTime
                        val advancedMode = uiState.advancedMode


                        showTimeValidationMessage(context, fromTime, toTime, advancedMode)




                        Log.d("MainActivity", "Permission check - Usage: $hasUsageStats, Overlay: $hasOverlay, Accessibility: $hasAccessibility")

                        when {
                            !hasUsageStats -> {
                                Log.d("MainActivity", "Missing usage stats permission")
                                currentPermissionStep = PermissionStep.USAGE_STATS
                                isWaitingForPermission = true
                                showPermissionDialog = true
                            }
                            !hasOverlay -> {
                                Log.d("MainActivity", "Missing overlay permission")
                                currentPermissionStep = PermissionStep.OVERLAY
                                isWaitingForPermission = true
                                showPermissionDialog = true
                            }
                            !hasAccessibility -> {
                                Log.d("MainActivity", "Missing accessibility permission")
                                currentPermissionStep = PermissionStep.ACCESSIBILITY
                                isWaitingForPermission = true
                                showPermissionDialog = true
                            }
                            else -> {
                                // All permissions granted
                                Log.d("MainActivity", "All permissions granted, starting service immediately")
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
                        Log.d("MainActivity", "Disabling blocking, stopping service")
                        scope.launch {
                            viewModel.setBlockingEnabled(false)
                            stopMonitoringService()
                            Toast.makeText(context, "FocusR Service Stopped.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        item {
            // Modern Settings Section
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
                }
            )

//            ModernSettingsSection( // for timer
//                advancedMode = uiState.advancedMode,
//                fromTime = uiState.fromTime,
//                toTime = uiState.toTime,
//                onAdvancedModeToggle = { scope.launch { viewModel.setAdvancedMode(it) } },
//                onFromTimeClick = {
//                    isFromPicker = true
//                    showTimePicker = true
//                },
//                onToTimeClick = {
//                    isFromPicker = false
//                    showTimePicker = true
//                },
//
//                simpleTimerLabel = "Focus duration",
//                simpleTimerSelected = simpleTimerSelected,
//                onSimpleTimerSelected = { simpleTimerSelected = it }
//            )
        }

        item {
            // Modern Blocked Apps Section
            ModernBlockedAppsSection(
                blockedApps = uiState.blockedApps,
                onAddApps = { showAppSelection = true },
                onRemoveApp = { packageName ->
                    scope.launch {
                        val updatedApps = uiState.blockedApps.toMutableSet()
                        updatedApps.remove(packageName)
                        viewModel.setBlockedApps(updatedApps)
                    }
                }
            )
        }

//        item {
//            // Quick Stats Section (New Feature)
//            ModernStatsSection()
//        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val activeState = if (isFromPicker) fromTimeState else toTimeState

        ModernTimePickerDialog(
            isFromPicker = isFromPicker,
            timePickerState = activeState,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                scope.launch {
                    if (isFromPicker) {
                        viewModel.setFromTime(activeState.hour, activeState.minute)
                    } else {
                        viewModel.setToTime(activeState.hour, activeState.minute)
                    }
                }
                showTimePicker = false
            }
        )
    }

    // App Selection Dialog
    if (showAppSelection) {
        ModernAppSelectionDialog(
            onDismiss = { showAppSelection = false },
            onAppsSelected = { selectedApps ->
                scope.launch {
                    viewModel.setBlockedApps(selectedApps)
                }
                showAppSelection = false
            },
            currentlyBlocked = uiState.blockedApps
        )
    }

    // Sequential Permission Dialog
    if (showPermissionDialog) {
        ModernPermissionDialog(
            step = currentPermissionStep,
            onDismiss = {
                Log.d("MainActivity", "Permission dialog dismissed")
                showPermissionDialog = false
                isWaitingForPermission = false
                if (uiState.blockingEnabled) {
                    scope.launch { viewModel.setBlockingEnabled(false) }
                }
            },
            onGrantPermission = {
                Log.d("MainActivity", "Grant permission clicked for step: $currentPermissionStep")
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
        )
    }
}

@Composable
fun ModernStatusCard(
    isEnabled: Boolean,
    pulseScale: Float,
    onToggle: (Boolean) -> Unit
) {
    val cardColors = if (isEnabled) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale),
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                        if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
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
                        tint = if (enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
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
                    textAlign = TextAlign.Center // Center aligns within the text box
                )

                Text(
                    text = if (isEnabled) "Your focus is protected from distractions"
                    else "Tap the switch below to enable protection",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = if (!isEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.scale(1.2f)
                )

                Text(
                    text = "ON",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                    color = if (isEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
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
    onToTimeClick: () -> Unit
) { val cardColors = if (advancedMode) {
    CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    )
} else {
    CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Advanced Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Switch(
                    checked = advancedMode,
                    onCheckedChange = onAdvancedModeToggle
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (advancedMode) {
                ModernTimeButton(
                    icon = Icons.Outlined.PlayArrow,
                    label = "Block FROM",
                    time = formatTime(fromTime.first, fromTime.second),
                    onClick = onFromTimeClick,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModernTimeButton(
                    icon = Icons.Outlined.Stop,
                    label = "Block TO",
                    time = formatTime(toTime.first, toTime.second),
                    onClick = onToTimeClick,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                ModernTimeButton(

                    icon = Icons.Outlined.Schedule,
                    label = "Block apps until (Select time onward till Midnight)",
                    time = formatTime(toTime.first, toTime.second),
                    onClick = onToTimeClick,
                    color = MaterialTheme.colorScheme.primary
                )

            }
        }
    }
}

//@Composable
//fun ModernSettingsSection( // for timer
//    advancedMode: Boolean,
//    fromTime: Pair<Int, Int>,
//    toTime: Pair<Int, Int>,
//    onAdvancedModeToggle: (Boolean) -> Unit,
//    onFromTimeClick: () -> Unit,
//    onToTimeClick: () -> Unit,
//    // Updated props for timer selection in simple mode:
//    simpleTimerLabel: String,
//    simpleTimerSelected: String,
//    onSimpleTimerSelected: (String) -> Unit,
//) {
//    val cardColors = if (advancedMode) {
//        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
//    } else {
//        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
//    }
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(20.dp),
//        colors = cardColors,
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Column(modifier = Modifier.padding(20.dp)) {
//            // Header row with advanced mode toggle
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(
//                        imageVector = Icons.Outlined.Settings,
//                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        "Advanced Mode",
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                }
//                Switch(checked = advancedMode, onCheckedChange = onAdvancedModeToggle)
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            if (advancedMode) {
//                // Existing from/to time pickers
//                ModernTimeButton(
//                    icon = Icons.Outlined.PlayArrow,
//                    label = "Block FROM",
//                    time = formatTime(fromTime.first, fromTime.second),
//                    onClick = onFromTimeClick,
//                    color = MaterialTheme.colorScheme.tertiary
//                )
//                Spacer(modifier = Modifier.height(12.dp))
//                ModernTimeButton(
//                    icon = Icons.Outlined.Stop,
//                    label = "Block TO",
//                    time = formatTime(toTime.first, toTime.second),
//                    onClick = onToTimeClick,
//                    color = MaterialTheme.colorScheme.secondary
//                )
//            } else {
//                // Simple mode with preset timer dropdown
//                TimerDropdownModern(
//                    label = simpleTimerLabel,
//                    selected = simpleTimerSelected,
//                    onSelected = onSimpleTimerSelected
//                )
//            }
//        }
//    }
//}
//@Composable
//fun TimerDropdownModern(
//    label: String,
//    selected: String,
//    onSelected: (String) -> Unit
//) {
//    val durations = listOf("5m", "10m", "15m", "30m", "45m", "1h", "2h", "3h")
//    var expanded by remember { mutableStateOf(false) }
//
//    Column {
//        Text(
//            text = label,
//            style = MaterialTheme.typography.labelSmall,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//
//        Spacer(modifier = Modifier.height(6.dp))
//
//        Surface(
//            shape = RoundedCornerShape(16.dp),
//            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
//            color = Color.Transparent,
//            modifier = Modifier
//                .fillMaxWidth()
//                .clip(RoundedCornerShape(16.dp))
//                .clickable { expanded = true }
//                .height(60.dp)
//                .padding(horizontal = 16.dp, vertical = 8.dp)
//        ) {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceBetween,
//                modifier = Modifier.fillMaxSize()
//            ) {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Icon(
//                        imageVector = Icons.Outlined.Schedule,
//                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = selected,
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.primary
//                    )
//                }
//
//                Icon(
//                    imageVector = Icons.Default.ArrowDropDown,
//                    contentDescription = "Open dropdown",
//                    tint = MaterialTheme.colorScheme.primary
//                )
//            }
//        }
//
//        // Duration dropdown
//        DropdownMenu(
//            expanded = expanded,
//            onDismissRequest = { expanded = false }
//        ) {
//            durations.forEach { option ->
//                DropdownMenuItem(
//                    text = { Text(option) },
//                    onClick = {
//                        onSelected(option)
//                        expanded = false
//                    },
//                    leadingIcon = {
//                        if (option == selected) {
//                            Icon(
//                                Icons.Default.Check,
//                                contentDescription = null,
//                                tint = MaterialTheme.colorScheme.primary
//                            )
//                        }
//                    }
//                )
//            }
//        }
//    }
//}


@Composable
fun ModernTimeButton(
    icon: ImageVector,
    label: String,
    time: String,
    onClick: () -> Unit,
    color: Color
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, color.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color
        )
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ModernBlockedAppsSection(
    blockedApps: Set<String>,
    onAddApps: () -> Unit,
    onRemoveApp: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Blocked Apps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${blockedApps.size} apps selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onAddApps,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,  // background color
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer   // text/icon color
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Select")
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
                            onRemove = { onRemoveApp(packageName) }
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No apps selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
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
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

//@Composable
//fun ModernStatsSection() {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(20.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Column(modifier = Modifier.padding(20.dp)) {
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Icon(
//                    imageVector = Icons.Outlined.Analytics,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.primary
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    "Today's Focus Stats (Hard Coded)",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.SemiBold
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                StatItem(
//                    icon = Icons.Outlined.Timer,
//                    value = "2h 30m",
//                    label = "Protected Time",
//                    color = MaterialTheme.colorScheme.primary
//                )
//                StatItem(
//                    icon = Icons.Outlined.Block,
//                    value = "12",
//                    label = "Blocks Today",
//                    color = MaterialTheme.colorScheme.error
//                )
//                StatItem(
//                    icon = Icons.Outlined.TrendingUp,
//                    value = "+15%",
//                    label = "Productivity",
//                    color = MaterialTheme.colorScheme.tertiary
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun StatItem(
//    icon: ImageVector,
//    value: String,
//    label: String,
//    color: Color
//) {
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(4.dp)
//    ) {
//        Icon(
//            imageVector = icon,
//            contentDescription = null,
//            tint = color,
//            modifier = Modifier.size(20.dp)
//        )
//        Text(
//            text = value,
//            style = MaterialTheme.typography.titleMedium,
//            fontWeight = FontWeight.Bold,
//            color = color
//        )
//        Text(
//            text = label,
//            style = MaterialTheme.typography.bodySmall,
//            color = MaterialTheme.colorScheme.onSurfaceVariant,
//            textAlign = TextAlign.Center
//        )
//    }
//}

@Composable
fun ModernTimePickerDialog(
    isFromPicker: Boolean,
    timePickerState: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFromPicker) "Select FROM time" else "Select TO time",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                TimePicker(state = timePickerState)

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel")
                    }
                    FilledTonalButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Confirm")
                    }
                }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.onPrimary.copy(0.9f),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                if (step != PermissionStep.COMPLETED) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress indicator
                    val progress = when (step) {
                        PermissionStep.USAGE_STATS -> 0.33f
                        PermissionStep.OVERLAY -> 0.66f
                        PermissionStep.ACCESSIBILITY -> 1f
                        else -> 1f
                    }

                    Column {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Step ${when(step) {
                                PermissionStep.USAGE_STATS -> "1"
                                PermissionStep.OVERLAY -> "2"
                                PermissionStep.ACCESSIBILITY -> "3"
                                else -> "3"
                            }} of 3",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onGrantPermission,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(buttonText)
            }
        },
        dismissButton = if (step != PermissionStep.COMPLETED) {
            {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Cancel")
                }
            }
        } else null,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ModernAppSelectionDialog(
    onDismiss: () -> Unit,
    onAppsSelected: (Set<String>) -> Unit,
    currentlyBlocked: Set<String>
) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var selectedApps by remember { mutableStateOf(currentlyBlocked) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        installedApps = AppManager.getInstalledApps(context)
        isLoading = false
    }

    val filteredApps = installedApps.filter { app ->
        app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Apps to Block",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selected count
                Text(
                    text = "${selectedApps.size} apps selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps) { app ->
                            ModernAppSelectionItem(
                                app = app,
                                isSelected = selectedApps.contains(app.packageName),
                                onSelectionChange = { isSelected ->
                                    selectedApps = if (isSelected) {
                                        selectedApps + app.packageName
                                    } else {
                                        selectedApps - app.packageName
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Cancel")
                        }
                        FilledTonalButton(
                            onClick = { onAppsSelected(selectedApps) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,  // background color
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer   // text/icon color
                            )
                        ) {
                            Text("Save (${selectedApps.size})")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernAppSelectionItem(
    app: AppInfo,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )

            Spacer(modifier = Modifier.width(12.dp))

            if (app.icon != null) {
                androidx.compose.foundation.Image(
                    bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper data class for the permission dialog
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = ComponentName(context, FocusBlockerAccessibilityService::class.java)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabledServicesSetting.split(":").any {
        ComponentName.unflattenFromString(it) == expectedComponentName
    }
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





