@file:OptIn(ExperimentalMaterial3Api::class)

package com.focusr.v2.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusr.v2.R
import com.focusr.v2.ui.screens.getCurrentTime
import kotlinx.coroutines.delay

//@Composable
//fun ModernTopBar() {
//    var timeString by remember { mutableStateOf(getCurrentTime()) }
//
//    LaunchedEffect(Unit) {
//        while (true) {
//            timeString = getCurrentTime()
//            delay(1000)
//        }
//    }
//
//    CenterAlignedTopAppBar(
//        title = {
//            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                Row {
//                    Image(
//                        painter = painterResource(id = R.drawable.self_improvement),
//                        contentDescription = "FocusR Logo",
//                        modifier = Modifier
//                            .height(48.dp)
//                            .padding(vertical = 4.dp)
//                    )
//                    Text(
//                        "FocusR",
//                        fontSize = 36.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color(0xFF2D9CDB),
//                        modifier = Modifier
//                            .height(48.dp)
//                            .padding(vertical = 4.dp)
//                    )
//                }
//
//                Text(
//                    "Current Time $timeString",
//                    fontSize = 14.sp,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        },
//        colors = TopAppBarDefaults.topAppBarColors(
//            containerColor = Color.Transparent
//        )
//    )
//}

@Composable
fun ModernTopBar(primaryAccent: Color) {
    var timeString by remember { mutableStateOf(getCurrentTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            timeString = getCurrentTime()
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.1f)
                    )
                )
            )
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
//            .backdrop(BlurRadius.Medium)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row {
                        Image(
                            painter = painterResource(id = R.drawable.self_improvement),
                            contentDescription = "FocusR Logo",
                            modifier = Modifier
                                .height(48.dp)
                                .padding(vertical = 4.dp)
                        )
                        Text(
                            "FocusR",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent,
                            modifier = Modifier
                                .height(48.dp)
                                .padding(vertical = 4.dp)
                        )
                    }

                    Text(
                        "Current Time $timeString",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}