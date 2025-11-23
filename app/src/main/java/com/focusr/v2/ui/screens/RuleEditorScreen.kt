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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.focusr.v2.AppInfo
import com.focusr.v2.models.BlockingRule
import com.focusr.v2.models.DayOfWeek
import com.focusr.v2.models.RuleType
import com.focusr.v2.ui.viewmodels.RuleViewModel
import java.util.*

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
    
    // SIMPLE rule state
    var blockUntilHour by remember { mutableStateOf(22) }
    var blockUntilMinute by remember { mutableStateOf(0) }
    
    // SCHEDULED rule state
    var fromHour by remember { mutableStateOf(9) }
    var fromMinute by remember { mutableStateOf(0) }
    var toHour by remember { mutableStateOf(17) }
    var toMinute by remember { mutableStateOf(0) }
    var selectedDays by remember { mutableStateOf(DayOfWeek.values().toSet()) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var existingRule by remember { mutableStateOf<BlockingRule?>(null) }
    
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
                            it.blockUntilTime?.let { (h, m) ->
                                blockUntilHour = h
                                blockUntilMinute = m
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
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ruleId == null) "Add Rule" else "Edit Rule") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (ruleId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
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
            OutlinedTextField(
                value = ruleName,
                onValueChange = { ruleName = it },
                label = { Text("Rule Name (optional)") },
                placeholder = { Text("e.g., Social Media Block") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // App Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAppPicker = true },
                shape = RoundedCornerShape(12.dp)
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
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = if (selectedApps.isEmpty()) 
                                "Tap to select apps" 
                            else 
                                "${selectedApps.size} app(s) selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Text("Rule Enabled")
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it }
                )
            }
            
            HorizontalDivider()
            
            // Rule Type Selector
            Text(
                text = "Rule Type",
                style = MaterialTheme.typography.titleSmall
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = ruleType == RuleType.SIMPLE,
                    onClick = { ruleType = RuleType.SIMPLE },
                    label = { Text("Simple") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = ruleType == RuleType.SCHEDULED,
                    onClick = { ruleType = RuleType.SCHEDULED },
                    label = { Text("Scheduled") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Text(
                text = when (ruleType) {
                    RuleType.SIMPLE -> "Block until a specific time today"
                    RuleType.SCHEDULED -> "Block during specific hours on selected days"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            HorizontalDivider()
            
            // Rule Configuration based on type
            when (ruleType) {
                RuleType.SIMPLE -> {
                    SimpleRuleConfig(
                        hour = blockUntilHour,
                        minute = blockUntilMinute,
                        onTimeChange = { h, m ->
                            blockUntilHour = h
                            blockUntilMinute = m
                        }
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
                            blockUntilTime = Pair(blockUntilHour, blockUntilMinute)
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
                    }
                    
                    if (ruleId == null) {
                        ruleViewModel.addRule(rule)
                    } else {
                        ruleViewModel.updateRule(rule)
                    }
                    
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedApps.isNotEmpty()
            ) {
                Text(if (ruleId == null) "Add Rule" else "Save Changes")
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

@Composable
fun SimpleRuleConfig(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Block Until",
            style = MaterialTheme.typography.titleSmall
        )
        
        TimePickerRow(
            hour = hour,
            minute = minute,
            onTimeChange = onTimeChange
        )
        
        Text(
            text = "Apps will be blocked until ${formatTimeDisplay(hour, minute)} today",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Text(text = "From", style = MaterialTheme.typography.titleSmall)
            TimePickerRow(hour = fromHour, minute = fromMinute, onTimeChange = onFromTimeChange)
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "To", style = MaterialTheme.typography.titleSmall)
            TimePickerRow(hour = toHour, minute = toMinute, onTimeChange = onToTimeChange)
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Days of Week", style = MaterialTheme.typography.titleSmall)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = hour.toString(),
            onValueChange = { 
                it.toIntOrNull()?.let { h ->
                    if (h in 0..23) onTimeChange(h, minute)
                }
            },
            label = { Text("Hour") },
            modifier = Modifier.weight(1f)
        )
        
        Text(":", style = MaterialTheme.typography.headlineMedium)
        
        OutlinedTextField(
            value = minute.toString().padStart(2, '0'),
            onValueChange = {
                it.toIntOrNull()?.let { m ->
                    if (m in 0..59) onTimeChange(hour, m)
                }
            },
            label = { Text("Minute") },
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = formatTimeDisplay(hour, minute),
            style = MaterialTheme.typography.bodyLarge,
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