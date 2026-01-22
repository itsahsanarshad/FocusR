package com.focusr.v2.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.focusr.v2.models.BlockingRule
import com.focusr.v2.models.RuleType
import com.focusr.v2.navigation.Screen
import com.focusr.v2.ui.viewmodels.RuleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRulesScreen(
    navController: NavController,
    ruleViewModel: RuleViewModel
) {
    val rulesState by ruleViewModel.uiState.collectAsState()
    val allRules = rulesState.allRules
    
    // Glassmorphism colors
    val primaryAccent = Color(0xFF6C63FF)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A1A2E),
            Color(0xFF16213E)
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E1E30).copy(alpha = 0.95f),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manage Rules",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${allRules.size} ${if (allRules.size == 1) "rule" else "rules"}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Create New Rule Button
                item {
                    Button(
                        onClick = { navController.navigate(Screen.RuleEditor.createRoute()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryAccent
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Create New Rule",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Rules List
                if (allRules.isEmpty()) {
                    item {
                        EmptyRulesPlaceholder()
                    }
                } else {
                    items(allRules, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            onEdit = {
                                navController.navigate(Screen.RuleEditor.createRoute(ruleId = rule.id))
                            },
                            onDelete = {
                                ruleViewModel.deleteRule(rule.id)
                            },
                            onToggleEnabled = {
                                ruleViewModel.toggleRuleEnabled(rule.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RuleCard(
    rule: BlockingRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val cardColor = if (rule.enabled) {
        Color(0xFF2A2A40).copy(alpha = 0.9f)
    } else {
        Color(0xFF1E1E30).copy(alpha = 0.7f)
    }
    
    val accentColor = when (rule.ruleType) {
        RuleType.SIMPLE -> Color(0xFFFFB84D)  // Orange for SIMPLE
        RuleType.SCHEDULED -> Color(0xFF6C63FF)  // Purple for SCHEDULED
        RuleType.MENTAL_CLARITY -> Color(0xFF4ECDC4)  // Teal for MENTAL_CLARITY
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Icon
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.2f),
                    border = BorderStroke(2.dp, accentColor)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (rule.ruleType) {
                                RuleType.SIMPLE -> Icons.Outlined.Schedule
                                RuleType.SCHEDULED -> Icons.Outlined.CalendarMonth
                                RuleType.MENTAL_CLARITY -> Icons.Outlined.Bedtime
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Rule Name & Type
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.getDisplayName(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rule.enabled) Color.White else Color.White.copy(alpha = 0.5f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = rule.ruleType.name,
                            fontSize = 12.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rule.getAppCountDescription(),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Enable/Disable Switch
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onToggleEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                        uncheckedTrackColor = Color(0xFF3A3A50)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Rule Description
            Text(
                text = rule.getDescription(),
                fontSize = 14.sp,
                color = if (rule.enabled) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 52.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Edit Button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accentColor
                    ),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 14.sp)
                }
                
                // Delete Button
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF6B6B)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 14.sp)
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Rule?") },
            text = { Text("Are you sure you want to delete \"${rule.getDisplayName()}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF6B6B))
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
fun EmptyRulesPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Rule,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.White.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Rules Yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first rule to start blocking apps",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}
