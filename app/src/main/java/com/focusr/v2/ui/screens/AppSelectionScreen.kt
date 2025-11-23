@file:OptIn(ExperimentalAnimationApi::class)

package com.focusr.v2.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.focusr.v2.AppInfo
import com.focusr.v2.models.BlockingRule
import com.focusr.v2.navigation.Screen
import com.focusr.v2.ui.viewmodels.RuleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAppSelectionScreen(
    onBackClick: () -> Unit,
    navController: NavController,
    ruleViewModel: RuleViewModel,
    availableApps: List<AppInfo>,
    selectionMode: Boolean = false,  // NEW
    ruleId: String? = null,  // NEW
    onAppsSelected: ((Set<String>) -> Unit)? = null  // NEW callback
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedApps by remember { mutableStateOf(setOf<String>()) }  // NEW

    // Load existing selections if editing
    LaunchedEffect(ruleId) {
        if (ruleId != null && selectionMode) {
            ruleViewModel.getRuleById(ruleId)?.let { rule ->
                selectedApps = rule.getApps().toSet()
            }
        }
    }
    
    // Collect rules state
    val rulesState by ruleViewModel.uiState.collectAsState()
    val rulesByPackage = remember(rulesState.allRules) {
        rulesState.allRules.groupBy { it.packageName }
    }
 
    // Filter apps
    val filteredApps = remember(availableApps, searchQuery) {
        if (searchQuery.isEmpty()) {
            availableApps
        } else {
            availableApps.filter { app ->
                app.appName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Separate apps with rules from apps without rules (only when not in selection mode)
    val (appsWithRules, appsWithoutRules) = remember(filteredApps, rulesByPackage, selectionMode) {
        if (selectionMode) {
            // In selection mode, show all apps in one list
            Pair(emptyList(), filteredApps.sortedBy { it.appName.lowercase() })
        } else {
            val withRules = filteredApps.filter { rulesByPackage[it.packageName]?.isNotEmpty() == true }
                .sortedBy { it.appName.lowercase() }
            val withoutRules = filteredApps.filter { rulesByPackage[it.packageName]?.isEmpty() != false }
                .sortedBy { it.appName.lowercase() }
            Pair(withRules, withoutRules)
        }
    }

    // Modern gradient background
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
                    radius = 1000f
                )
            )
    ) {
        // Glassmorphism background elements
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6C63FF).copy(alpha = 0.1f),
                            Color(0xFF4ECDC4).copy(alpha = 0.1f),
                            Color(0xFFFF6B6B).copy(alpha = 0.1f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Modern Top Bar with Glassmorphism
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back Button
                        Surface(
                            onClick = onBackClick,
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF2D3748),
                            shadowElevation = 8.dp,
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.6f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Title Section
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (selectionMode) "Select Apps" else "Manage Apps",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = Color.White
                            )
                            
                            // Show selection count or apps with rules count
                            if (selectionMode && selectedApps.isNotEmpty()) {
                                Text(
                                    text = "${selectedApps.size} selected",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            } else if (!selectionMode) {
                                Text(
                                    text = "${appsWithRules.size} apps with rules",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Done button for selection mode or placeholder for symmetry
                        if (selectionMode) {
                            Surface(
                                onClick = {
                                    // Save selections and go back
                                    onAppsSelected?.invoke(selectedApps)
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("selected_apps", selectedApps.toList())
                                    onBackClick()
                                },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFF6C63FF),
                                shadowElevation = 8.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }

            // Content Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar with Glassmorphism
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search apps...",
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Apps List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Apps with Rules Section (only in normal mode)
                    if (appsWithRules.isNotEmpty() && !selectionMode) {
                        item {
                            ModernSectionHeader(
                                title = "Apps with Rules",
                                count = appsWithRules.size,
                                icon = Icons.Outlined.Block,
                                color = Color(0xFF6C63FF)
                            )
                        }
                        items(appsWithRules) { app ->
                            val rules = rulesByPackage[app.packageName] ?: emptyList()
                            ModernAppItemWithRules(
                                appInfo = app,
                                rules = rules,
                                onAddRule = {
                                    navController.navigate(
                                        Screen.RuleEditor.createRoute()
                                    )
                                },
                                onViewRules = {
                                    // Navigate to first rule for editing
                                    val firstRule = rules.firstOrNull()
                                    if (firstRule != null) {
                                        navController.navigate(
                                            Screen.RuleEditor.createRoute(
                                                ruleId = firstRule.id
                                            )
                                        )
                                    }
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // All Apps Section
                    if (appsWithoutRules.isNotEmpty()) {
                        item {
                            ModernSectionHeader(
                                title = if (selectionMode) "Select Apps" else "All Apps",
                                count = appsWithoutRules.size,
                                icon = Icons.Outlined.Apps,
                                color = Color(0xFF4ECDC4)
                            )
                        }
                        items(appsWithoutRules) { app ->
                            if (selectionMode) {
                                // Selection mode: show selectable items
                                ModernAppItemSelectable(
                                    appInfo = app,
                                    isSelected = selectedApps.contains(app.packageName),
                                    onClick = {
                                        selectedApps = if (selectedApps.contains(app.packageName)) {
                                            selectedApps - app.packageName
                                        } else {
                                            selectedApps + app.packageName
                                        }
                                    }
                                )
                            } else {
                                // Normal mode: show with rules
                                ModernAppItemWithRules(
                                    appInfo = app,
                                    rules = emptyList(),
                                    onAddRule = {
                                        navController.navigate(
                                            Screen.RuleEditor.createRoute()
                                        )
                                    },
                                    onViewRules = {}
                                )
                            }
                        }
                    }

                    // Empty State
                    if (appsWithRules.isEmpty() && appsWithoutRules.isEmpty()) {
                        item {
                            ModernEmptyState(
                                searchQuery = searchQuery,
                                hasApps = availableApps.isNotEmpty()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernSectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = color.copy(alpha = 0.3f)
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ModernAppItemWithRules(
    appInfo: AppInfo,
    rules: List<BlockingRule>,
    onAddRule: () -> Unit,
    onViewRules: () -> Unit
) {
    val hasRules = rules.isNotEmpty()
    val activeRules = rules.count { it.enabled }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (hasRules) onViewRules() else onAddRule() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasRules)
                Color.White.copy(alpha = 0.25f)
            else
                Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon with Glassmorphism
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                if (appInfo.icon != null) {
                    AsyncImage(
                        model = appInfo.icon,
                        contentDescription = "${appInfo.appName} icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = "App icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // App Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (hasRules) {
                    Text(
                        text = "$activeRules active ${if (activeRules == 1) "rule" else "rules"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6C63FF),
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "No rules",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action Button
            if (hasRules) {
                // Rule count badge
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color(0xFF6C63FF)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = rules.size.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            } else {
                // Add Rule button
                IconButton(
                    onClick = onAddRule,
                    modifier = Modifier.size(40.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = Color(0xFF6C63FF).copy(alpha = 0.3f),
                        border = BorderStroke(2.dp, Color(0xFF6C63FF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Rule",
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

// NEW: Selectable app item for selection mode
@Composable
fun ModernAppItemSelectable(
    appInfo: AppInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                Color(0xFF6C63FF).copy(alpha = 0.3f)
            else
                Color.White.copy(alpha = 0.1f)
        ),
        border = if (isSelected)
            BorderStroke(2.dp, Color(0xFF6C63FF))
        else
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon with Glassmorphism
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                if (appInfo.icon != null) {
                    AsyncImage(
                        model = appInfo.icon,
                        contentDescription = "${appInfo.appName} icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = "App icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // App Info
            Text(
                text = appInfo.appName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Selection Indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF6C63FF),
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Not selected",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ModernEmptyState(
    searchQuery: String,
    hasApps: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = when {
                    searchQuery.isNotEmpty() -> Icons.Default.SearchOff
                    !hasApps -> Icons.Outlined.Apps
                    else -> Icons.Default.Apps
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.4f)
            )
            Text(
                text = when {
                    searchQuery.isNotEmpty() -> "No apps found"
                    !hasApps -> "No apps available"
                    else -> "Start adding rules"
                },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Text(
                text = when {
                    searchQuery.isNotEmpty() -> "Try a different search term"
                    !hasApps -> "No apps found on this device"
                    else -> "Tap any app to create a blocking rule"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}