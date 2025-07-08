//@file:OptIn(ExperimentalMaterial3Api::class)
//
//package com.focusr.v2.ui.components
//
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.Card
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.FilledTonalButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedButton
//import androidx.compose.material3.Text
//import androidx.compose.material3.TimePicker
//import androidx.compose.material3.TimePickerState
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.window.Dialog
//import androidx.compose.ui.window.DialogProperties
//
//@Composable
//fun ModernTimePickerDialog(
//    isFromPicker: Boolean,
//    timePickerState: TimePickerState,
//    onDismiss: () -> Unit,
//    onConfirm: () -> Unit
//) {
//    Dialog(
//        onDismissRequest = onDismiss,
//        properties = DialogProperties(usePlatformDefaultWidth = false)
//    ) {
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            shape = RoundedCornerShape(28.dp)
//        ) {
//            Column(
//                modifier = Modifier.padding(24.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    text = if (isFromPicker) "Select FROM time" else "Select TO time",
//                    style = MaterialTheme.typography.headlineSmall,
//                    fontWeight = FontWeight.Bold
//                )
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                TimePicker(state = timePickerState)
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    OutlinedButton(
//                        onClick = onDismiss,
//                        modifier = Modifier.weight(1f),
//                        shape = RoundedCornerShape(16.dp)
//                    ) {
//                        Text("Cancel")
//                    }
//                    FilledTonalButton(
//                        onClick = onConfirm,
//                        modifier = Modifier.weight(1f),
//                        shape = RoundedCornerShape(16.dp)
//                    ) {
//                        Text("Confirm")
//                    }
//                }
//            }
//        }
//    }
//}


@file:OptIn(ExperimentalMaterial3Api::class)

package com.focusr.v2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.commandiron.wheel_picker_compose.core.TimeFormat
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import com.commandiron.wheel_picker_compose.WheelTimePicker
import java.time.LocalTime

@Composable
fun ModernTimePickerDialog(
    isFromPicker: Boolean,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    // Local state to track the selected time from the wheel picker
    var selectedTime by remember { mutableStateOf(LocalTime.of(initialHour, initialMinute)) }

    // Design tokens for glassmorphism theme
    val primaryGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
    )
    val glassCard = Color.White.copy(alpha = 0.15f)
    val primaryAccent = Color(0xFF6C63FF)
    val secondaryAccent = Color(0xFF4ECDC4)
    val glassButton = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.4f),
            Color.White.copy(alpha = 0.2f)
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Single glass card containing everything
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = primaryAccent.copy(alpha = 0.2f)
                ),
                border = BorderStroke(
                    width = 3.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.2f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isFromPicker) "Select FROM time" else "Select TO time",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Large wheel picker directly on glass card
                    WheelTimePicker(
                        startTime = selectedTime,
                        timeFormat = TimeFormat.AM_PM,
                        rowCount = 3,
                        textColor = Color.White,
                        textStyle = MaterialTheme.typography.titleMedium,
                        selectorProperties = WheelPickerDefaults.selectorProperties(
                            color = primaryAccent.copy(alpha = 0.2f),
                            border = BorderStroke(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(primaryAccent, secondaryAccent)
                                )
                            )
                        ),
                        onSnappedTime = {
                            selectedTime = it
                        }
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Cancel button with glassmorphism (no border)
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = glassButton,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Confirm button with gradient accent
                        Button(
                            onClick = {
                                onConfirm(selectedTime.hour, selectedTime.minute)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(primaryAccent, secondaryAccent)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Confirm",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



