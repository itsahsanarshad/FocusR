package com.focusr.v2.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusr.v2.models.BlockingRule
import com.focusr.v2.models.RuleType
import kotlinx.coroutines.delay

/**
 * Status card showing rule statistics
 */
@Composable
fun RuleStatusCard(
    totalRules: Int,
    activeRules: Int,
    blockedApps: Int,
    nextActivation: String?,
    isPaused: Boolean,
    pauseUntil: Long?,
    modifier: Modifier = Modifier
) {
    // ADD THIS: Force recomposition every minute to update countdown
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(isPaused, pauseUntil) {
        if (isPaused && pauseUntil != null && pauseUntil != Long.MAX_VALUE) {
            while (true) {
                delay(60000) // Update every minute
                currentTime = System.currentTimeMillis()
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A40).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📊 Status",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                if (isPaused) {
                    Surface(
                        color = Color(0xFFFFB84D),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "⏸ Paused",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Rules", totalRules.toString(), Icons.Outlined.Rule)
                StatItem("Active", activeRules.toString(), Icons.Outlined.CheckCircle)
                StatItem("Blocked", blockedApps.toString(), Icons.Outlined.Block)
            }
            
            // Next activation
            if (nextActivation != null && !isPaused) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "⏰ $nextActivation",
                    fontSize = 14.sp,
                    color = Color(0xFF6C63FF),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Pause info
if (isPaused && pauseUntil != null && pauseUntil != Long.MAX_VALUE) {
    Spacer(modifier = Modifier.height(12.dp))
    val remaining = (pauseUntil - currentTime) / 1000 / 60
    val hours = remaining / 60
    val mins = remaining % 60
    
    val timeText = when {
        hours > 0 && mins > 0 -> "Resumes in ${hours}h ${mins}m"
        hours > 0 -> "Resumes in ${hours}h"
        mins > 0 -> "Resumes in ${mins}m"
        else -> "Resuming..."
    }
    
    Text(
        timeText,
        fontSize = 14.sp,
        color = Color(0xFFFFB84D),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Medium
    )
}
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6C63FF),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Dialog for selecting pause duration
 */
@Composable
fun PauseAllDialog(
    onDismiss: () -> Unit,
    onPause: (Int?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2A2A40),
        title = {
            Text(
                "⏸ Pause All Rules",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "How long would you like to pause blocking?",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Duration options
                PauseDurationButton("15 minutes", 15, onPause, onDismiss)
                Spacer(modifier = Modifier.height(8.dp))
                PauseDurationButton("30 minutes", 30, onPause, onDismiss)
                Spacer(modifier = Modifier.height(8.dp))
                PauseDurationButton("1 hour", 60, onPause, onDismiss)
                Spacer(modifier = Modifier.height(8.dp))
                PauseDurationButton("Until I resume", null, onPause, onDismiss)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
fun PauseDurationButton(
    label: String,
    minutes: Int?,
    onPause: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    Button(
        onClick = {
            onPause(minutes)
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF6C63FF)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, fontSize = 16.sp)
    }
}

/**
 * Section showing currently active rules
 */
@Composable
fun ActiveRulesSection(
    activeRules: List<BlockingRule>,
    modifier: Modifier = Modifier
) {
    if (activeRules.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A40).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "⚡ Active Rules",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            activeRules.forEach { rule ->
                ActiveRuleItem(rule)
                if (rule != activeRules.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ActiveRuleItem(rule: BlockingRule) {
    // Pulsing animation for active indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (rule.ruleType == RuleType.SIMPLE) 
                Icons.Outlined.Schedule 
            else 
                Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = Color(0xFF6C63FF),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                rule.getDisplayName(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                "Blocking ${rule.getApps().size} apps",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        
        // Pulsing indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    Color(0xFF4CAF50).copy(alpha = alpha),
                    CircleShape
                )
        )
    }
}
