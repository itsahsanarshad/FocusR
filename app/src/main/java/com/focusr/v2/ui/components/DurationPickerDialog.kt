package com.focusr.v2.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focusr.v2.utils.formatDuration


@Composable
fun DurationPickerDialog(
    currentDuration: Int,
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hours by remember { mutableIntStateOf(currentDuration / 60) }
    var minutes by remember { mutableIntStateOf(currentDuration % 60) }
    var textValue by remember { mutableStateOf(hours.toString()) }
    var isFocused by remember { mutableStateOf(false) }

    var minutesTextValue by remember { mutableStateOf(minutes.toString()) }
    var minutesIsFocused by remember { mutableStateOf(false) }

    // Function to normalize time (handle overflow/underflow)
    fun normalizeTime() {
        when {
            minutes >= 60 -> {
                hours += minutes / 60
                minutes %= 60
            }
            minutes < 0 -> {
                val hoursToSubtract = (-minutes + 59) / 60
                hours -= hoursToSubtract
                minutes = 60 - ((-minutes) % 60)
                if (minutes == 60) minutes = 0
            }
        }

        // Ensure hours don't go negative
        if (hours < 0) {
            hours = 0
            minutes = 0
        }

        // Optional: Set maximum limit (e.g., 24 hours)
        if (hours > 11) {
            hours = 11
            minutes = 0
        }
    }

    // Sync textValue with hours when not focused
    LaunchedEffect(hours) {
        if (!isFocused) {
            textValue = hours.toString()
        }
    }

    // Sync minutesTextValue with minutes when not focused
    LaunchedEffect(minutes) {
        if (!minutesIsFocused) {
            minutesTextValue = minutes.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Custom Duration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Set your custom blocking duration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hours picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Hours",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    hours -= 1
                                    normalizeTime()
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease hours")
                            }

                            BasicTextField(
                                value = textValue,
                                onValueChange = { newValue ->
                                    // Always update text while editing
                                    textValue = newValue

                                    // Only update hours if it's a valid number
                                    if (newValue.isNotEmpty() && newValue.all { it.isDigit() }) {
                                        val parsed = newValue.toIntOrNull()
                                        if (parsed != null && parsed >= 0 && parsed <= 99) {
                                            hours = parsed
                                            normalizeTime()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .width(48.dp)
                                    .onFocusChanged { focusState ->
                                        isFocused = focusState.isFocused

                                        // When losing focus, handle empty or invalid states
                                        if (!focusState.isFocused) {
                                            when {
                                                textValue.isEmpty() -> {
                                                    hours = 0
                                                    textValue = "0"
                                                }
                                                !textValue.all { it.isDigit() } -> {
                                                    // Reset to current hours value if invalid
                                                    textValue = hours.toString()
                                                }
                                            }
                                        }
                                    }
                                    .clickable { /* Focus will be handled automatically */ },
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                )
                            )

                            IconButton(
                                onClick = {
                                    hours += 1
                                    normalizeTime()
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase hours")
                            }
                        }
                    }

                    Text(
                        ":",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Minutes picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Minutes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    minutes -= 5 // Changed to 15 for consistency
                                    normalizeTime()
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease minutes")
                            }

                            BasicTextField(
                                value = minutesTextValue,
                                onValueChange = { newValue ->
                                    // Always update text while editing
                                    minutesTextValue = newValue

                                    // Only update minutes if it's a valid number
                                    if (newValue.isNotEmpty() && newValue.all { it.isDigit() }) {
                                        val parsed = newValue.toIntOrNull()
                                        if (parsed != null && parsed >= 0 && parsed <= 59) { // Minutes: 0-59
                                            minutes = parsed
                                            normalizeTime()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .width(48.dp)
                                    .onFocusChanged { focusState ->
                                        minutesIsFocused = focusState.isFocused

                                        // When losing focus, handle empty or invalid states
                                        if (!focusState.isFocused) {
                                            when {
                                                minutesTextValue.isEmpty() -> {
                                                    minutes = 0
                                                    minutesTextValue = "0"
                                                }
                                                !minutesTextValue.all { it.isDigit() } -> {
                                                    // Reset to current minutes value if invalid
                                                    minutesTextValue = minutes.toString()
                                                }
                                            }
                                        }
                                    }
                                    .clickable { /* Focus will be handled automatically */ },
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                )
                            )

                            IconButton(
                                onClick = {
                                    minutes += 5 // Changed to 15 for consistency
                                    normalizeTime()
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase minutes")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Total: ${formatDuration(hours * 60 + minutes)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val totalMinutes = hours * 60 + minutes
                    if (totalMinutes > 0) {
                        onDurationSelected(totalMinutes)
                        onDismiss()
                    }
                }
            ) {
                Text("Set Duration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



@Composable
fun DurationPresetChip(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .background(
                if (isSelected) {
                    color.copy(alpha = 0.2f)
                } else {
                    color.copy(alpha = 0.1f)
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) color else color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatDuration(minutes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

