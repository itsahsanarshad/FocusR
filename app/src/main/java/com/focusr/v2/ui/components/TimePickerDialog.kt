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

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.commandiron.wheel_picker_compose.core.TimeFormat
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import com.commandiron.wheel_picker_compose.WheelTimePicker
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import java.time.LocalTime
import androidx.core.graphics.drawable.toDrawable

/*
Add these dependencies to your app/build.gradle:

dependencies {
    implementation 'com.eightbitlab:blurview:1.6.6'
    implementation 'androidx.renderscript:renderscript-toolkit:21.0.0'
}

Add these imports at the top of your file:
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
*/

@Composable
fun ModernTimePickerDialog(
    isFromPicker: Boolean,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedTime by remember { mutableStateOf(LocalTime.of(initialHour, initialMinute)) }
    val context = LocalContext.current
    val activity = context as? Activity
    val currentView = LocalView.current

    // Design tokens
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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // BlurView container that blurs the background
        AndroidView(
            factory = { context ->
                BlurView(context).apply {
                    // Get the root view to blur
                    val rootView: ViewGroup? = activity?.window?.decorView?.findViewById(android.R.id.content)

                    rootView?.let { root ->
                        setupWith(root, RenderScriptBlur(context))
                            .setFrameClearDrawable(Color.Transparent.toArgb().toDrawable())
                            .setBlurRadius(8f)
                            .setOverlayColor(Color.Black.copy(alpha = 0.7f).toArgb())
                            .setBlurAutoUpdate(true)
                    }

                    // Set layout params for full screen
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { blurView ->
            // The BlurView will automatically blur the background
            // Now we add our dialog content as a regular Android View
            blurView.removeAllViews()

            // Create a container for our Compose content
            val composeView = androidx.compose.ui.platform.ComposeView(context).apply {
                setContent {
                    // Dialog content - this will NOT be blurred
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        DialogContent(
                            isFromPicker = isFromPicker,
                            selectedTime = selectedTime,
                            onTimeChange = { selectedTime = it },
                            onDismiss = onDismiss,
                            onConfirm = onConfirm,
                            glassCard = glassCard,
                            primaryAccent = primaryAccent,
                            secondaryAccent = secondaryAccent,
                            glassButton = glassButton
                        )
                    }
                }
            }

            blurView.addView(composeView)
        }
    }
}

@Composable
private fun DialogContent(
    isFromPicker: Boolean,
    selectedTime: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    glassCard: Color,
    primaryAccent: Color,
    secondaryAccent: Color,
    glassButton: Brush
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glass card containing the time picker
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp)),
            colors = CardDefaults.cardColors(
                containerColor = glassCard
            ),
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.1f)
                    )
                )
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = if (isFromPicker) "Select FROM time" else "Select TO time",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Time picker
                WheelTimePicker(
                    startTime = selectedTime,
                    timeFormat = TimeFormat.AM_PM,
                    rowCount = 3,
                    textColor = Color.White,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    selectorProperties = WheelPickerDefaults.selectorProperties(
                        color = primaryAccent.copy(alpha = 0.2f),
                        border = BorderStroke(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(primaryAccent, secondaryAccent)
                            )
                        )
                    ),
                    onSnappedTime = onTimeChange
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cancel button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = glassButton,
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Confirm button
                    Button(
                        onClick = {
                            onConfirm(selectedTime.hour, selectedTime.minute)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(primaryAccent, secondaryAccent)
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Confirm",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}



