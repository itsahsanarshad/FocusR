package com.focusr.v2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner  // ADD THIS
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.focusr.v2.AppInfo
import com.focusr.v2.models.BlockingRule
import com.focusr.v2.models.DayOfWeek
import com.focusr.v2.models.RuleType
import com.focusr.v2.navigation.Screen  // ADD THIS
import com.focusr.v2.ui.viewmodels.RuleViewModel
import java.util.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleEditorScreen(
    ruleId: String? = null,
    ruleViewModel: RuleViewModel,
    navController: NavController,
    availableApps: List<AppInfo>
) {
    var ruleType by remember { mutableStateOf(RuleType.SIMPLE) }
    var enabled by remember { mutableStateOf(true) }
    var ruleName by remember { mutableStateOf("") }
    var selectedApps by remember { mutableStateOf(setOf<String>()) }
    var showAppPicker by remember { mutableStateOf(false) }
    
    // SIMPLE rule state (duration-based)
    var selectedDuration by remember { mutableStateOf(60) } // Default 1 hour
    
    // SCHEDULED rule state
    var fromHour by remember { mutableStateOf(9) }
    var fromMinute by remember { mutableStateOf(0) }
    var toHour by remember { mutableStateOf(17) }
    var toMinute by remember { mutableStateOf(0) }
    var selectedDays by remember { mutableStateOf(DayOfWeek.values().toSet()) }
    
    // MENTAL_CLARITY rule state
    var windDownHour by remember { mutableStateOf(21) }  // 9 PM default
    var windDownMinute by remember { mutableStateOf(0) }
    var windDownEnabled by remember { mutableStateOf(true) }
    var morningBlockDuration by remember { mutableStateOf(120) }  // 2 hours default
    var morningFuryEnabled by remember { mutableStateOf(true) }
    var sleepDetectionMinutes by remember { mutableStateOf(300) }  // 5 hours default
    var morningWindowStart by remember { mutableStateOf(4) }  // 4 AM
    var morningWindowEnd by remember { mutableStateOf(12) }  // 12 PM
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var existingRule by remember { mutableStateOf<BlockingRule?>(null) }
    var showAppPickerDialog by remember { mutableStateOf(false) }
    
    // Load existing rule if editing
    LaunchedEffect(ruleId) {
        if (ruleId != null) {
            ruleViewModel.uiState.collect { state ->
                val rule = state.allRules.find { it.id == ruleId }
                rule?.let {
                    existingRule = it
                    ruleType = it.ruleType
                    enabled = it.enabled
                    ruleName = it.name
                    selectedApps = it.getApps().toSet()
                    
                    when (it.ruleType) {
                        RuleType.SIMPLE -> {
                            it.durationMinutes?.let { duration ->
                                selectedDuration = duration
                            }
                        }
                        RuleType.SCHEDULED -> {
                            it.fromTime?.let { (h, m) ->
                                fromHour = h
                                fromMinute = m
                            }
                            it.toTime?.let { (h, m) ->
                                toHour = h
                                toMinute = m
                            }
                            selectedDays = it.daysOfWeek
                        }
                        RuleType.MENTAL_CLARITY -> {
                            it.windDownTime?.let { (h, m) ->
                                windDownHour = h
                                windDownMinute = m
                            }
                            windDownEnabled = it.windDownEnabled
                            it.morningBlockDuration?.let { duration ->
                                morningBlockDuration = duration
                            }
                            morningFuryEnabled = it.morningFuryEnabled
                            sleepDetectionMinutes = it.sleepDetectionMinutes
                            morningWindowStart = it.morningWindowStart
                            morningWindowEnd = it.morningWindowEnd
                            selectedDays = it.daysOfWeek
                        }
                    }
                }
            }
        }
    }

    // At the top of RuleEditorScreen, after LaunchedEffect for loading rule
// Observe selected apps from navigation
val lifecycleOwner = LocalLifecycleOwner.current
LaunchedEffect(Unit) {
    navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<List<String>>("selected_apps")
        ?.observe(lifecycleOwner) { apps ->
            selectedApps = apps.toSet()
        }
}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    ),
                    radius = 1200f
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (ruleId == null) "Add Rule" else "Edit Rule",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        if (ruleId != null) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF6B6B))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF2A2A40).copy(alpha = 0.9f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rule Name Input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A2A40).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    TextField(
                        value = ruleName,
                        onValueChange = { ruleName = it },
                        label = { Text("Rule Name (optional)", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("e.g., Study Mode", color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6C63FF),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF6C63FF)
                        )
                    )
                }

                // App Selection Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAppPickerDialog = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A2A40).copy(alpha = 0.9f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Target Apps",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (selectedApps.isEmpty())
                                    "Tap to select apps"
                                else
                                    "${selectedApps.size} app(s) selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Show selected apps as chips
                if (selectedApps.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedApps.forEach { pkgName ->
                            val app = availableApps.find { it.packageName == pkgName }
                            AssistChip(
                                onClick = { selectedApps = selectedApps - pkgName },
                                label = { Text(app?.appName ?: pkgName) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Enable/Disable toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rule Enabled",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }

                HorizontalDivider()

                // Rule Type Selector
                Text(
                    text = "Rule Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scalable rule type selector
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(RuleType.entries.size) { index ->
                        val type = RuleType.entries[index]
                        val isSelected = ruleType == type
                        Card(
                            onClick = { ruleType = type },
                            modifier = Modifier
                                .width(150.dp)
                                .height(80.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    Color(0xFF6C63FF)
                                else
                                    Color(0xFF2A2A40).copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (isSelected)
                                BorderStroke(2.dp, Color(0xFF6C63FF).copy(alpha = 0.5f))
                            else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = when (type) {
                                        RuleType.SIMPLE -> Icons.Outlined.Schedule
                                        RuleType.SCHEDULED -> Icons.Outlined.CalendarMonth
                                        RuleType.MENTAL_CLARITY -> Icons.Outlined.Bedtime
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (type) {
                                        RuleType.SIMPLE -> "Simple"
                                        RuleType.SCHEDULED -> "Scheduled"
                                        RuleType.MENTAL_CLARITY -> "Mental Clarity"
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (ruleType) {
                        RuleType.SIMPLE -> "Block for a selected duration"
                        RuleType.SCHEDULED -> "Block during specific hours on selected days"
                        RuleType.MENTAL_CLARITY -> "Smart blocking: wind-down at night & morning focus"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                HorizontalDivider()

                // Rule Configuration based on type
                when (ruleType) {
                    RuleType.SIMPLE -> {
                        DurationRuleConfig(
                            selectedDuration = selectedDuration,
                            onDurationChange = { selectedDuration = it }
                        )
                    }

                    RuleType.SCHEDULED -> {
                        ScheduledRuleConfig(
                            fromHour = fromHour,
                            fromMinute = fromMinute,
                            toHour = toHour,
                            toMinute = toMinute,
                            selectedDays = selectedDays,
                            onFromTimeChange = { h, m ->
                                fromHour = h
                                fromMinute = m
                            },
                            onToTimeChange = { h, m ->
                                toHour = h
                                toMinute = m
                            },
                            onDaysChange = { selectedDays = it }
                        )
                    }
                    
                    RuleType.MENTAL_CLARITY -> {
                        MentalClarityRuleConfig(
                            windDownHour = windDownHour,
                            windDownMinute = windDownMinute,
                            windDownEnabled = windDownEnabled,
                            morningBlockDuration = morningBlockDuration,
                            morningFuryEnabled = morningFuryEnabled,
                            sleepDetectionMinutes = sleepDetectionMinutes,
                            morningWindowStart = morningWindowStart,
                            morningWindowEnd = morningWindowEnd,
                            selectedDays = selectedDays,
                            onWindDownTimeChange = { h, m ->
                                windDownHour = h
                                windDownMinute = m
                            },
                            onWindDownEnabledChange = { windDownEnabled = it },
                            onMorningDurationChange = { morningBlockDuration = it },
                            onMorningFuryEnabledChange = { morningFuryEnabled = it },
                            onSleepDetectionChange = { sleepDetectionMinutes = it },
                            onMorningWindowChange = { start, end ->
                                morningWindowStart = start
                                morningWindowEnd = end
                            },
                            onDaysChange = { selectedDays = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = {
                        val rule = when (ruleType) {
                            RuleType.SIMPLE -> BlockingRule(
                                id = ruleId ?: UUID.randomUUID().toString(),
                                name = ruleName,
                                packageNames = selectedApps.toList(),
                                ruleType = RuleType.SIMPLE,
                                enabled = enabled,
                                durationMinutes = selectedDuration,
                                activatedAt = System.currentTimeMillis()
                            )

                            RuleType.SCHEDULED -> BlockingRule(
                                id = ruleId ?: UUID.randomUUID().toString(),
                                name = ruleName,
                                packageNames = selectedApps.toList(),
                                ruleType = RuleType.SCHEDULED,
                                enabled = enabled,
                                fromTime = Pair(fromHour, fromMinute),
                                toTime = Pair(toHour, toMinute),
                                daysOfWeek = selectedDays
                            )
                            
                            RuleType.MENTAL_CLARITY -> BlockingRule(
                                id = ruleId ?: UUID.randomUUID().toString(),
                                name = ruleName.ifEmpty { "Mental Clarity" },
                                packageNames = selectedApps.toList(),
                                ruleType = RuleType.MENTAL_CLARITY,
                                enabled = enabled,
                                windDownTime = Pair(windDownHour, windDownMinute),
                                windDownEnabled = windDownEnabled,
                                morningBlockDuration = morningBlockDuration,
                                morningFuryEnabled = morningFuryEnabled,
                                sleepDetectionMinutes = sleepDetectionMinutes,
                                morningWindowStart = morningWindowStart,
                                morningWindowEnd = morningWindowEnd,
                                daysOfWeek = selectedDays
                            )
                        }

                        if (ruleId == null) {
                            ruleViewModel.addRule(rule)
                        } else {
                            ruleViewModel.updateRule(rule)
                        }

                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = selectedApps.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C63FF),
                        disabledContainerColor = Color(0xFF2A2A40).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (ruleId == null) Icons.Default.Add else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (ruleId == null) "Add Rule" else "Save Changes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Validation message
                if (selectedApps.isEmpty()) {
                    Text(
                        text = "Please select at least one app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

        }
    }

    // App Picker Dialog
    if (showAppPickerDialog) {
        Dialog(
            onDismissRequest = { showAppPickerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ModernAppSelectionScreen(
                    onBackClick = { showAppPickerDialog = false },
                    navController = navController,
                    ruleViewModel = ruleViewModel,
                    availableApps = availableApps,
                    selectionMode = true,
                    ruleId = ruleId,
                    initialSelectedApps = selectedApps,  // ADD THIS - pass current selection
                    onAppsSelected = { apps ->
                        selectedApps = apps
                        showAppPickerDialog = false
                    }
                )
            }
        }
    }
    // App Picker Dialog
    if (showAppPicker) {
        AppPickerDialog(
            availableApps = availableApps,
            selectedApps = selectedApps,
            onSelectionChange = { selectedApps = it },
            onDismiss = { showAppPicker = false }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Rule?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ruleId?.let { ruleViewModel.deleteRule(it) }
                        navController.popBackStack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AppPickerDialog(
    availableApps: List<AppInfo>,
    selectedApps: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(availableApps, searchQuery) {
        if (searchQuery.isEmpty()) {
            availableApps.sortedBy { it.appName.lowercase() }
        } else {
            availableApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true)
            }.sortedBy { it.appName.lowercase() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("Select Apps (${selectedApps.size} selected)") 
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = selectedApps.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectionChange(
                                        if (isSelected) {
                                            selectedApps - app.packageName
                                        } else {
                                            selectedApps + app.packageName
                                        }
                                    )
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    onSelectionChange(
                                        if (isSelected) {
                                            selectedApps - app.packageName
                                        } else {
                                            selectedApps + app.packageName
                                        }
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationRuleConfig(
    selectedDuration: Int,
    onDurationChange: (Int) -> Unit
) {
    // Preset durations in minutes
    val durations = listOf(
        15 to "15m",
        30 to "30m",
        60 to "1h",
        120 to "2h",
        180 to "3h",
        240 to "4h",
        360 to "6h",
        480 to "8h"
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Block Duration",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            durations.forEach { (minutes, label) ->
                val isSelected = selectedDuration == minutes
                FilterChip(
                    selected = isSelected,
                    onClick = { onDurationChange(minutes) },
                    label = { 
                        Text(
                            label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF6C63FF),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF2A2A40),
                        labelColor = Color.White.copy(alpha = 0.8f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.White.copy(alpha = 0.3f),
                        selectedBorderColor = Color(0xFF6C63FF),
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
        
        // Show selected duration description
        val durationText = when {
            selectedDuration < 60 -> "${selectedDuration} minutes"
            selectedDuration == 60 -> "1 hour"
            selectedDuration % 60 == 0 -> "${selectedDuration / 60} hours"
            else -> "${selectedDuration / 60}h ${selectedDuration % 60}m"
        }
        
        Text(
            text = "Blocking will last for $durationText from when the rule is saved",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ScheduledRuleConfig(
    fromHour: Int,
    fromMinute: Int,
    toHour: Int,
    toMinute: Int,
    selectedDays: Set<DayOfWeek>,
    onFromTimeChange: (Int, Int) -> Unit,
    onToTimeChange: (Int, Int) -> Unit,
    onDaysChange: (Set<DayOfWeek>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "From", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)


            TimePickerRow(hour = fromHour, minute = fromMinute, onTimeChange = onFromTimeChange)
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "To", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TimePickerRow(hour = toHour, minute = toMinute, onTimeChange = onToTimeChange)
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Days of Week", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))
            DaysOfWeekSelector(selectedDays = selectedDays, onDaysChange = onDaysChange)
        }
    }
}

@Composable
fun TimePickerRow(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    var hourText by remember { mutableStateOf(hour.toString()) }
    var minuteText by remember { mutableStateOf(minute.toString().padStart(2, '0')) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // HOUR
        OutlinedTextField(
            value = hourText,
            onValueChange = { newValue ->
                hourText = newValue.filter { it.isDigit() }     // allow delete

                val h = hourText.toIntOrNull()
                if (h != null && h in 0..23) {
                    onTimeChange(h, minute)
                }
            },
            label = { Text("Hour") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )

        Text(":", style = MaterialTheme.typography.headlineMedium)

        // MINUTE
        OutlinedTextField(
            value = minuteText,
            onValueChange = { newValue ->
                minuteText = newValue.filter { it.isDigit() }

                val m = minuteText.toIntOrNull()
                if (m != null && m in 0..59) {
                    onTimeChange(hour, m)
                }
            },
            label = { Text("Minute") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = formatTimeDisplay(hour, minute),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaysOfWeekSelector(
    selectedDays: Set<DayOfWeek>,
    onDaysChange: (Set<DayOfWeek>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onDaysChange(DayOfWeek.values().toSet()) },
                modifier = Modifier.weight(1f)
            ) { Text("All", style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(
                onClick = { onDaysChange(DayOfWeek.weekdays()) },
                modifier = Modifier.weight(1f)
            ) { Text("Weekdays", style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(
                onClick = { onDaysChange(DayOfWeek.weekend()) },
                modifier = Modifier.weight(1f)
            ) { Text("Weekend", style = MaterialTheme.typography.bodySmall) }
        }
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DayOfWeek.values().forEach { day ->
                FilterChip(
                    selected = selectedDays.contains(day),
                    onClick = {
                        onDaysChange(
                            if (selectedDays.contains(day)) selectedDays - day
                            else selectedDays + day
                        )
                    },
                    label = { Text(day.shortName) }
                )
            }
        }
    }
}

private fun formatTimeDisplay(hour: Int, minute: Int): String {
    val period = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%d:%02d %s", displayHour, minute, period)
}

/**
 * Configuration UI for Mental Clarity rules (wind-down + morning focus)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MentalClarityRuleConfig(
    windDownHour: Int,
    windDownMinute: Int,
    windDownEnabled: Boolean,
    morningBlockDuration: Int,
    morningFuryEnabled: Boolean,
    sleepDetectionMinutes: Int,
    morningWindowStart: Int,
    morningWindowEnd: Int,
    selectedDays: Set<DayOfWeek>,
    onWindDownTimeChange: (Int, Int) -> Unit,
    onWindDownEnabledChange: (Boolean) -> Unit,
    onMorningDurationChange: (Int) -> Unit,
    onMorningFuryEnabledChange: (Boolean) -> Unit,
    onSleepDetectionChange: (Int) -> Unit,
    onMorningWindowChange: (Int, Int) -> Unit,
    onDaysChange: (Set<DayOfWeek>) -> Unit
) {
    var showWindDownTimePicker by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Wind-down Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A40).copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🌙",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Wind Down",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Switch(
                        checked = windDownEnabled,
                        onCheckedChange = onWindDownEnabledChange
                    )
                }
                
                if (windDownEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Block apps after this time",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showWindDownTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(formatTimeDisplay(windDownHour, windDownMinute))
                    }
                }
            }
        }
        
        // Morning Fury Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A40).copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "☀️",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Morning Focus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Switch(
                        checked = morningFuryEnabled,
                        onCheckedChange = onMorningFuryEnabledChange
                    )
                }
                
                if (morningFuryEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Block apps for how long after waking up?",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Duration presets
                    val durations = listOf(
                        30 to "30m",
                        60 to "1h",
                        90 to "1.5h",
                        120 to "2h",
                        180 to "3h"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durations.forEach { (minutes, label) ->
                            FilterChip(
                                selected = morningBlockDuration == minutes,
                                onClick = { onMorningDurationChange(minutes) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }
        
        // Sleep Detection Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A40).copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "😴",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sleep Detection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Consider it sleep if no phone usage for:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val sleepThresholds = listOf(
                    180 to "3h",
                    240 to "4h",
                    300 to "5h",
                    360 to "6h"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sleepThresholds.forEach { (minutes, label) ->
                        FilterChip(
                            selected = sleepDetectionMinutes == minutes,
                            onClick = { onSleepDetectionChange(minutes) },
                            label = { Text(label) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Detect wake-up between:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start hour
                    val startHours = listOf(3, 4, 5, 6)
                    startHours.forEach { hour ->
                        FilterChip(
                            selected = morningWindowStart == hour,
                            onClick = { onMorningWindowChange(hour, morningWindowEnd) },
                            label = { Text("${hour}AM") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // End hour
                    val endHours = listOf(10, 11, 12)
                    endHours.forEach { hour ->
                        FilterChip(
                            selected = morningWindowEnd == hour,
                            onClick = { onMorningWindowChange(morningWindowStart, hour) },
                            label = { Text(if (hour == 12) "12PM" else "${hour}AM") }
                        )
                    }
                }
            }
        }
        
        // Days Selection
        Text(
            text = "Active Days",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        DaysOfWeekSelector(
            selectedDays = selectedDays,
            onDaysChange = onDaysChange
        )
    }
    
    // Wind-down time picker dialog
    if (showWindDownTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = windDownHour,
            initialMinute = windDownMinute
        )
        AlertDialog(
            onDismissRequest = { showWindDownTimePicker = false },
            title = { Text("Wind Down Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    onWindDownTimeChange(timePickerState.hour, timePickerState.minute)
                    showWindDownTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWindDownTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}