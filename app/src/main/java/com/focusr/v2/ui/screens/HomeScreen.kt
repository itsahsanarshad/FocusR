@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)

package com.focusr.v2.ui.screens

import android.app.ActivityManager
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.focusr.v2.AppInfo
import com.focusr.v2.AppManager
import com.focusr.v2.AppMonitoringService
//import com.focusr.v2.FocusBlockerAccessibilityService
import com.focusr.v2.MainActivity
import com.focusr.v2.PermissionHelper
import com.focusr.v2.PermissionHelper.hasIgnoreBatteryOptimizationsPermission
import com.focusr.v2.PermissionHelper.requestIgnoreBatteryOptimizations
import com.focusr.v2.PreferencesManager
import com.focusr.v2.R
import com.focusr.v2.ServiceScheduler
import com.focusr.v2.navigation.Screen
import com.focusr.v2.ui.components.ModernTimeButton
import com.focusr.v2.ui.components.ModernTimePickerDialog
import com.focusr.v2.ui.components.ModernTopBar
enum class PermissionStep {
    USAGE_STATS,
    OVERLAY,
    ACCESSIBILITY,

    BATTERY_OPTIMIZATION,  // new step added here
    COMPLETED
}


@Composable
fun HomeScreen(activity: MainActivity, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember(context) { PreferencesManager(context.applicationContext) }
    var onResumeCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    // ADD THIS STATE
    var isEnabled by remember { mutableStateOf(false) }

    var showTimePicker by remember { mutableStateOf(false) }
    var isFromPicker by remember { mutableStateOf(true) }
    var showAppSelection by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var currentPermissionStep by remember { mutableStateOf(PermissionStep.COMPLETED) }
    var isWaitingForPermission by remember { mutableStateOf(false) }

    val fromTimeState = rememberTimePickerState(
        is24Hour = false
    )
    val toTimeState = rememberTimePickerState(
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
                    currentPermissionStep = PermissionStep.BATTERY_OPTIMIZATION
                    showPermissionDialog = true
                } else {
                    showPermissionDialog = true
                }
            }
            PermissionStep.BATTERY_OPTIMIZATION -> {
                if (hasIgnoreBatteryOptimizationsPermission(context)) {
                    currentPermissionStep = PermissionStep.COMPLETED
                    showPermissionDialog = false
                    isWaitingForPermission = false
                    // All permissions granted, enable the toggle
                    isEnabled = true
                    Toast.makeText(context, "All permissions granted! FocusR is ready.", Toast.LENGTH_SHORT).show()
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
                        isEnabled = isEnabled, // ADD THIS
                        pulseScale = 1f,
                        onToggle = { enabled ->
                            if (enabled) {
                                // Check all permissions
                                val hasUsageStats = PermissionHelper.hasUsageStatsPermission(context)
                                val hasOverlay = PermissionHelper.hasOverlayPermission(context)
                                val hasAccessibility = isAccessibilityServiceEnabled(context)
                                val hasBatteryOptimization = hasIgnoreBatteryOptimizationsPermission(context)

                                // Check if any permission is missing
                                val missingPermission = when {
                                    !hasUsageStats -> PermissionStep.USAGE_STATS
                                    !hasOverlay -> PermissionStep.OVERLAY
                                    !hasAccessibility -> PermissionStep.ACCESSIBILITY
                                    !hasBatteryOptimization -> PermissionStep.BATTERY_OPTIMIZATION
                                    else -> null
                                }

                                // If any permission is missing, show permission dialog
                                if (missingPermission != null) {
                                    currentPermissionStep = missingPermission
                                    isWaitingForPermission = true
                                    showPermissionDialog = true
                                } else {
                                    // All permissions granted, enable blocking
                                    isEnabled = true
                                    scope.launch {
                                        // Start your monitoring service here
                                        // startMonitoringService()
                                        Toast.makeText(context, "FocusR Service Started.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                // Disable blocking
                                isEnabled = false
                                scope.launch {
                                    // Stop your monitoring service here
                                    // stopMonitoringService()
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
                    // Manage Rules Button
                    Button(
                        onClick = { navController.navigate(Screen.ManageRules.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C63FF)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Rule,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Manage Rules",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (showPermissionDialog) {
                ModernPermissionDialog(
                    step = currentPermissionStep,
                    onDismiss = {
                        showPermissionDialog = false
                        isWaitingForPermission = false
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
                            PermissionStep.BATTERY_OPTIMIZATION -> {
                                requestIgnoreBatteryOptimizations(context)
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
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
fun ModernPermissionDialog(
    step: PermissionStep,
    onDismiss: () -> Unit,
    onGrantPermission: () -> Unit
) {
    val manufacturer = android.os.Build.MANUFACTURER.lowercase(Locale.getDefault())

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
        PermissionStep.BATTERY_OPTIMIZATION -> Quadruple(
            "Battery Optimization",
            "Please remove Focusr from battery optimizations so it can run reliably in the background.\n Step.1 Search ForcusR in All apps, Step.2 Remove From Optimization Mode, Enjoy your Focusr Journey",
            "Remove",
            Icons.Outlined.BatteryChargingFull  // Or a suitable battery icon
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
                            imageVector = icon,
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
                        PermissionStep.USAGE_STATS -> 0.25f
                        PermissionStep.OVERLAY -> 0.5f
                        PermissionStep.ACCESSIBILITY -> 0.75f
                        PermissionStep.BATTERY_OPTIMIZATION -> 1f
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
                                PermissionStep.BATTERY_OPTIMIZATION -> "4"
                                else -> "4"
                            }} of 4",
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
                                buttonText,
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




