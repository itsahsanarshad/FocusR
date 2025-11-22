package com.focusr.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focusr.v2.models.BlockingRule
import com.focusr.v2.models.DayOfWeek
import com.focusr.v2.models.RuleType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Simple test activity to verify rule-based blocking backend
 * This can be launched to manually test the system
 */
class TestRulesActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val preferencesManager = PreferencesManager(this)
        val blockingTimeManager = BlockingTimeManager(preferencesManager)
        
        setContent {
            MaterialTheme {
                TestRulesScreen(preferencesManager, blockingTimeManager)
            }
        }
    }
}

@Composable
fun TestRulesScreen(
    preferencesManager: PreferencesManager,
    blockingTimeManager: BlockingTimeManager
) {
    val scope = rememberCoroutineScope()
    var testResults by remember { mutableStateOf("") }
    var allRules by remember { mutableStateOf<List<BlockingRule>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        preferencesManager.blockingRules.collect { rules ->
            allRules = rules
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Rule-Based Blocking Test",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Test 1: Create Simple Rule
        Button(
            onClick = {
                scope.launch {
                    val rule = BlockingRule(
                        packageName = "com.instagram.android",
                        ruleType = RuleType.SIMPLE,
                        blockUntilTime = Pair(22, 0) // Until 10 PM
                    )
                    preferencesManager.addRule(rule)
                    testResults += "✅ Created SIMPLE rule for Instagram (until 10 PM)\n"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test 1: Create Simple Rule")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Test 2: Create Scheduled Rule
        Button(
            onClick = {
                scope.launch {
                    val rule = BlockingRule(
                        packageName = "com.youtube.android",
                        ruleType = RuleType.SCHEDULED,
                        fromTime = Pair(2, 0),  // 2 AM
                        toTime = Pair(5, 0),    // 5 AM
                        daysOfWeek = DayOfWeek.values().toSet()
                    )
                    preferencesManager.addRule(rule)
                    testResults += "✅ Created SCHEDULED rule for YouTube (2 AM - 5 AM daily)\n"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test 2: Create Scheduled Rule")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Test 3: Check if Instagram should be blocked
        Button(
            onClick = {
                scope.launch {
                    val shouldBlock = blockingTimeManager.shouldBlockApp("com.instagram.android")
                    testResults += "Instagram blocking status: ${if (shouldBlock) "🔴 BLOCKED" else "🟢 ALLOWED"}\n"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test 3: Check Instagram Status")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Test 4: Get Active Rules Count
        Button(
            onClick = {
                scope.launch {
                    val count = blockingTimeManager.getActiveRulesCount()
                    val blockedApps = blockingTimeManager.getCurrentlyBlockedApps()
                    testResults += "Active rules: $count\n"
                    testResults += "Blocked apps: ${blockedApps.joinToString(", ")}\n"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test 4: Get Active Rules Count")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Test 5: Test Migration
        Button(
            onClick = {
                scope.launch {
                    // Add some old-style blocked apps
                    preferencesManager.setBlockedApps(setOf("com.facebook.katana", "com.twitter.android"))
                    preferencesManager.setAdvancedMode(false)
                    preferencesManager.setToTime(20, 0)
                    
                    // Trigger migration
                    preferencesManager.migrateOldBlockedApps()
                    
                    testResults += "✅ Migration completed\n"
                    
                    // Check migrated rules
                    val fbRules = preferencesManager.getRulesForApp("com.facebook.katana")
                    val twRules = preferencesManager.getRulesForApp("com.twitter.android")
                    testResults += "Facebook rules: ${fbRules.size}\n"
                    testResults += "Twitter rules: ${twRules.size}\n"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test 5: Test Migration")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Clear All Rules
        Button(
            onClick = {
                scope.launch {
                    val rules = preferencesManager.blockingRules.first()
                    rules.forEach { preferencesManager.removeRule(it.id) }
                    testResults += "🗑️ Cleared all rules\n"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Clear All Rules")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Show all rules
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "All Rules (${allRules.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                allRules.forEach { rule ->
                    Text(
                        text = "• ${rule.packageName}: ${rule.getDescription()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Test Results
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Test Results",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = testResults.ifEmpty { "Run tests above to see results..." },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
