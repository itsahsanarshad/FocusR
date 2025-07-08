//package com.focusr.v2
//
//import android.content.Intent
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.animation.*
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.blur
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.window.Dialog
//import androidx.compose.ui.window.DialogProperties
//import com.focusr.v2.ui.theme.OpalForAndroidTheme
//import kotlinx.coroutines.delay
//import java.util.*
//
//class BlockerOverlayActivity : ComponentActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val blockedAppPackage = intent.getStringExtra("blocked_app") ?: ""
//
//        setContent {
//            OpalForAndroidTheme(darkTheme = false) {
//                ModernBlockerOverlay(
//                    blockedApp = blockedAppPackage,
//                    onChooseToClose = {
//                        closeBlockedApp()
//                        finish()
//                    }
//                )
//            }
//        }
//    }
//
//    private fun closeBlockedApp() {
//        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
//            addCategory(Intent.CATEGORY_HOME)
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//        }
//        startActivity(homeIntent)
//    }
//}
//
//@Composable
//fun ModernBlockerOverlay(
//    blockedApp: String,
//    onChooseToClose: () -> Unit
//) {
//    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
//    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
//    val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
//
//    // Context-aware reflective messages based on time and user behavior
//    val contextualMessages = getContextualMessages(currentHour, isWeekend)
//
//    val currentMessage = remember { contextualMessages.random() }
//    var showContent by remember { mutableStateOf(false) }
//    var buttonClicked by remember { mutableStateOf(false) }
//
//    LaunchedEffect(Unit) {
//        delay(500)
//        showContent = true
//    }
//
//    Dialog(
//        onDismissRequest = { }, // Don't dismiss on outside click
//        properties = DialogProperties(
//            dismissOnBackPress = false,
//            dismissOnClickOutside = false,
//            usePlatformDefaultWidth = false
//        )
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(
//                    Brush.verticalGradient(
//                        colors = listOf(
//                            Color(0xFFF0F9FF), // Light blue
//                            Color(0xFFE0F2FE), // Lighter blue
//                            Color(0xFFBAE6FD)  // Sky blue - matching your theme
//                        )
//                    )
//                )
//                .blur(if (showContent) 0.dp else 8.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            AnimatedVisibility(
//                visible = showContent,
//                enter = fadeIn(animationSpec = tween(1000)) + scaleIn(
//                    initialScale = 0.85f,
//                    animationSpec = tween(1000, easing = FastOutSlowInEasing)
//                ),
//                exit = fadeOut(animationSpec = tween(500))
//            ) {
//                ReflectiveCard(
//                    message = currentMessage,
//                    onChooseToClose = {
//                        buttonClicked = true
//                        onChooseToClose()
//                    },
//                    buttonClicked = buttonClicked
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun ReflectiveCard(
//    message: ReflectiveMessage,
//    onChooseToClose: () -> Unit,
//    buttonClicked: Boolean
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(24.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = Color.White.copy(alpha = 0.98f)
//        ),
//        shape = RoundedCornerShape(28.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .padding(36.dp)
//                .fillMaxWidth(),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            // Animated Icon with gentle breathing effect
//            val infiniteTransition = rememberInfiniteTransition(label = "icon_animation")
//            val iconScale by infiniteTransition.animateFloat(
//                initialValue = 1f,
//                targetValue = 1.12f,
//                animationSpec = infiniteRepeatable(
//                    animation = tween(2500, easing = FastOutSlowInEasing),
//                    repeatMode = RepeatMode.Reverse
//                ), label = "icon_scale"
//            )
//
//            val iconAlpha by infiniteTransition.animateFloat(
//                initialValue = 0.8f,
//                targetValue = 1f,
//                animationSpec = infiniteRepeatable(
//                    animation = tween(2500, easing = FastOutSlowInEasing),
//                    repeatMode = RepeatMode.Reverse
//                ), label = "icon_alpha"
//            )
//
//            Icon(
//                imageVector = message.actionIcon,
//                contentDescription = null,
//                modifier = Modifier
//                    .size(88.dp)
//                    .graphicsLayer(
//                        scaleX = iconScale,
//                        scaleY = iconScale,
//                        alpha = iconAlpha
//                    ),
//                tint = Color(0xFF0EA5E9) // Light blue matching your theme
//            )
//
//            Spacer(modifier = Modifier.height(28.dp))
//
//            // Main reflective question
//            Text(
//                text = message.mainText,
//                fontSize = 26.sp,
//                fontWeight = FontWeight.SemiBold,
//                color = Color(0xFF1E293B), // Dark slate
//                textAlign = TextAlign.Center,
//                lineHeight = 34.sp
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Subtitle with context
//            Text(
//                text = message.subText,
//                fontSize = 17.sp,
//                color = Color(0xFF64748B), // Slate gray
//                textAlign = TextAlign.Center,
//                lineHeight = 26.sp
//            )
//
//            Spacer(modifier = Modifier.height(36.dp))
//
//            // Single focus button - no continue option
//            Button(
//                onClick = onChooseToClose,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(60.dp),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color(0xFF0EA5E9) // Matching your theme blue
//                ),
//                shape = RoundedCornerShape(20.dp),
//                elevation = ButtonDefaults.buttonElevation(
//                    defaultElevation = if (buttonClicked) 2.dp else 8.dp
//                )
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.Center
//                ) {
//                    Icon(
//                        imageVector = message.actionIcon,
//                        contentDescription = null,
//                        modifier = Modifier.size(24.dp),
//                        tint = Color.White
//                    )
//                    Spacer(modifier = Modifier.width(12.dp))
//                    Text(
//                        text = message.actionText,
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = Color.White
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(20.dp))
//
//            // Motivational footer
//            Text(
//                text = message.motivationalFooter,
//                fontSize = 14.sp,
//                color = Color(0xFF0EA5E9).copy(alpha = 0.8f),
//                textAlign = TextAlign.Center,
//                fontWeight = FontWeight.Medium
//            )
//        }
//    }
//}
//
//@Composable
//fun getContextualMessages(hour: Int, isWeekend: Boolean): List<ReflectiveMessage> {
//    return when {
//        // Early Morning (5-8 AM)
//        hour in 5..8 -> listOf(
//            ReflectiveMessage(
//                mainText = "Good morning! Your day is just beginning.",
//                subText = "How you start sets the tone for everything ahead. What intention do you want to carry forward?",
//                actionText = "Start with Focus",
//                actionIcon = Icons.Default.WbSunny,
//                motivationalFooter = "Every great day starts with a mindful choice ☀️"
//            ),
//            ReflectiveMessage(
//                mainText = "The morning is your canvas.",
//                subText = "Before diving into distractions, take a moment to think about what truly matters to you today.",
//                actionText = "Choose Purpose",
//                actionIcon = Icons.Default.Palette,
//                motivationalFooter = "Paint your day with intention 🎨"
//            )
//        )
//
//        // Mid Morning (9-11 AM)
//        hour in 9..11 -> listOf(
//            ReflectiveMessage(
//                mainText = "The morning energy is still strong.",
//                subText = "This is when your mind is sharpest. Are you using this precious time for what matters most?",
//                actionText = "Harness Focus",
//                actionIcon = Icons.Default.Psychology,
//                motivationalFooter = "Your peak hours deserve your best attention 🧠"
//            ),
//            ReflectiveMessage(
//                mainText = "You're in the productivity zone.",
//                subText = "Right now, you could accomplish something meaningful. What would make you proud by lunch?",
//                actionText = "Choose Progress",
//                actionIcon = Icons.Default.TrendingUp,
//                motivationalFooter = "Small wins compound into big victories 📈"
//            )
//        )
//
//        // Midday (12-2 PM)
//        hour in 12..14 -> listOf(
//            ReflectiveMessage(
//                mainText = "Midday check-in: How are you feeling?",
//                subText = "Are you reaching for this app out of habit, or is there something deeper you're seeking?",
//                actionText = "Reflect & Refocus",
//                actionIcon = Icons.Default.SelfImprovement,
//                motivationalFooter = "Awareness is the first step to change 🧘"
//            ),
//            ReflectiveMessage(
//                mainText = "Half the day is behind you.",
//                subText = "What story do you want the second half to tell? Every moment is a chance to redirect.",
//                actionText = "Rewrite Your Day",
//                actionIcon = Icons.Default.Edit,
//                motivationalFooter = "You're the author of your own story 📝"
//            )
//        )
//
//        // Afternoon (3-5 PM)
//        hour in 15..17 -> listOf(
//            ReflectiveMessage(
//                mainText = "The afternoon dip is real.",
//                subText = "Instead of scrolling through the slump, what if you took a walk or did something energizing?",
//                actionText = "Choose Energy",
//                actionIcon = Icons.Default.DirectionsWalk,
//                motivationalFooter = "Movement beats mindless scrolling 🚶"
//            ),
//            ReflectiveMessage(
//                mainText = "You're feeling the pull of distraction.",
//                subText = "This feeling will pass. What would serve you better right now than endless scrolling?",
//                actionText = "Choose Wisely",
//                actionIcon = Icons.Default.Lightbulb,
//                motivationalFooter = "The urge is temporary, your goals are permanent 💡"
//            )
//        )
//
//        // Evening (6-8 PM)
//        hour in 18..20 -> listOf(
//            ReflectiveMessage(
//                mainText = "The day is winding down.",
//                subText = "Instead of losing yourself in the screen, what would help you truly unwind and connect?",
//                actionText = "Choose Connection",
//                actionIcon = Icons.Default.Favorite,
//                motivationalFooter = "Real connections beat digital distractions ❤️"
//            ),
//            ReflectiveMessage(
//                mainText = "Evening is for reflection.",
//                subText = if (isWeekend) "Weekend evenings are precious. How do you want to spend these peaceful hours?"
//                else "Work is done. This is your time. What would make you feel truly refreshed?",
//                actionText = "Choose Peace",
//                actionIcon = Icons.Default.Spa,
//                motivationalFooter = "True rest comes from mindful choices 🌙"
//            )
//        )
//
//        // Night (9-11 PM)
//        hour in 21..23 -> listOf(
//            ReflectiveMessage(
//                mainText = "Your mind needs to wind down.",
//                subText = "Blue light and endless feeds won't help you sleep better. What would prepare you for rest?",
//                actionText = "Choose Rest",
//                actionIcon = Icons.Default.Bedtime,
//                motivationalFooter = "Better sleep starts with better choices 😴"
//            ),
//            ReflectiveMessage(
//                mainText = "The day is almost over.",
//                subText = "Before you disappear into the scroll, take a moment to appreciate what you accomplished today.",
//                actionText = "Practice Gratitude",
//                actionIcon = Icons.Default.Star,
//                motivationalFooter = "Gratitude is the best way to end any day ⭐"
//            )
//        )
//
//        // Late Night (12-4 AM)
//        hour in 0..4 -> listOf(
//            ReflectiveMessage(
//                mainText = "It's very late. Your body needs rest.",
//                subText = "Late-night scrolling often leads to regret in the morning. What would your morning self thank you for?",
//                actionText = "Choose Sleep",
//                actionIcon = Icons.Default.NightlightRound,
//                motivationalFooter = "Tomorrow starts with tonight's choices 🌙"
//            ),
//            ReflectiveMessage(
//                mainText = "The night is for dreaming, not scrolling.",
//                subText = "Whatever you're looking for in this app, it will still be there tomorrow. Your sleep won't be.",
//                actionText = "Prioritize Rest",
//                actionIcon = Icons.Default.Bedtime,
//                motivationalFooter = "Your future self needs this rest 💤"
//            )
//        )
//
//        // Default case
//        else -> listOf(
//            ReflectiveMessage(
//                mainText = "Take a moment to pause.",
//                subText = "Before diving in, ask yourself: what am I really looking for right now?",
//                actionText = "Choose Mindfully",
//                actionIcon = Icons.Default.Psychology,
//                motivationalFooter = "Mindful choices create meaningful days ✨"
//            )
//        )
//    }
//}
//
//data class ReflectiveMessage(
//    val mainText: String,
//    val subText: String,
//    val actionText: String,
//    val actionIcon: ImageVector,
//    val motivationalFooter: String
//)



package com.focusr.v2

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.focusr.v2.ui.theme.OpalForAndroidTheme
import kotlinx.coroutines.delay
import java.util.*

class BlockerOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockedAppPackage = intent.getStringExtra("blocked_app") ?: ""
        val appCategory = getAppCategory(blockedAppPackage)
        Log.d("BlockerDebugBOA", appCategory.toString())

        setContent {
            OpalForAndroidTheme(darkTheme = false) {
                ModernBlockerOverlay(
                    blockedApp = blockedAppPackage,
                    appCategory = appCategory,
                    onChooseToClose = {
                        closeBlockedApp()
                        finish()
                    }
                )
            }
        }
    }

    private fun closeBlockedApp() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(homeIntent)
    }

    fun getAppCategory(packageName: String): AppCategory {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val category = applicationInfo.category
            Log.d("App Category Info", "Category value = $category")

            // Use Android native category if available
            when (category) {
                ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMING
                ApplicationInfo.CATEGORY_AUDIO -> AppCategory.ENTERTAINMENT
                ApplicationInfo.CATEGORY_VIDEO -> AppCategory.ENTERTAINMENT
                ApplicationInfo.CATEGORY_IMAGE -> AppCategory.GENERAL
                ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL_MEDIA
                ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
                ApplicationInfo.CATEGORY_MAPS -> AppCategory.GENERAL
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.GENERAL
                else -> getCategoryFromPackageName(packageName) // fallback
            }
        } catch (e: Exception) {
            Log.e("App Category Info", "Error: ${e.message}")
            AppCategory.GENERAL
        }
    }


//    private fun getCategoryFromPackageName(packageName: String): AppCategory {
//        return when {
//            // 🔹 Social Media Apps
//            packageName.contains("facebook") || packageName.contains("instagram") ||
//                    packageName.contains("twitter") || packageName.contains("tiktok") ||
//                    packageName.contains("snapchat") || packageName.contains("whatsapp") ||
//                    packageName.contains("telegram") || packageName.contains("discord") ||
//                    packageName.contains("reddit") || packageName.contains("linkedin") ->
//                AppCategory.SOCIAL_MEDIA
//
//            // 🔸 Entertainment/Video/Audio Apps
//            packageName.contains("netflix") || packageName.contains("youtube") ||
//                    packageName.contains("disney") || packageName.contains("hulu") ||
//                    packageName.contains("spotify") || packageName.contains("twitch") ||
//                    packageName.contains("video") || packageName.contains("media") ||
//                    packageName.contains("soundcloud") || packageName.contains("mxplayer") ->
//                AppCategory.ENTERTAINMENT
//
//            // 🎮 Gaming Apps
//            packageName.contains("game") || packageName.contains("play") ||
//                    packageName.contains("clash") || packageName.contains("candy") ||
//                    packageName.contains("pubg") || packageName.contains("pokemon") ||
//                    packageName.contains("mobilelegends") || packageName.contains("farlight") ||
//                    packageName.contains("freefire") ->
//                AppCategory.GAMING
//
//            // 🛍️ Shopping Apps (Expanded)
//            packageName.contains("amazon") || packageName.contains("ebay") ||
//                    packageName.contains("shop") || packageName.contains("buy") ||
//                    packageName.contains("store") || packageName.contains("mall") ||
//                    packageName.contains("daraz") || packageName.contains("alibaba") ||
//                    packageName.contains("aliexpress") || packageName.contains("temu") ||
//                    packageName.contains("noon") || packageName.contains("flipkart") ||
//                    packageName.contains("shein") || packageName.contains("walmart") ->
//                AppCategory.SHOPPING
//
//            // 📰 News Apps
//            packageName.contains("news") || packageName.contains("cnn") ||
//                    packageName.contains("bbc") || packageName.contains("times") ||
//                    packageName.contains("guardian") || packageName.contains("post") ||
//                    packageName.contains("tribune") || packageName.contains("nytimes") ->
//                AppCategory.NEWS
//
//
//
//            else -> AppCategory.GENERAL
//        }
//    }

    private fun getCategoryFromPackageName(packageName: String): AppCategory {
        val lower = packageName.lowercase()

        return when {
            // 🔹 Social Media Apps
            lower.contains("facebook") || lower.contains("instagram") ||
                    lower.contains("twitter") || lower.contains("tiktok") ||
                    lower.contains("snapchat")   ||
                    lower.contains("reddit") || lower.contains("linkedin") ->
                AppCategory.SOCIAL_MEDIA

            // 🔸 Entertainment / Video / Audio Apps
            lower.contains("netflix") || lower.contains("youtube") ||
                    lower.contains("disney") || lower.contains("hulu") ||
                    lower.contains("spotify") || lower.contains("twitch") ||
                    lower.contains("video") || lower.contains("media") ||
                    lower.contains("soundcloud") || lower.contains("mxplayer") ||
                    lower.contains("zee5") || lower.contains("hotstar") ->
                AppCategory.ENTERTAINMENT

            // 🎮 Gaming Apps
            lower.contains("game") || lower.contains("play") ||
                    lower.contains("clash") || lower.contains("candy") ||
                    lower.contains("pubg") || lower.contains("pokemon") ||
                    lower.contains("mobilelegends") || lower.contains("farlight") ||
                    lower.contains("freefire") || lower.contains("roblox") ->
                AppCategory.GAMING

            // 🛍️ Shopping Apps (Expanded)
            lower.contains("amazon") || lower.contains("ebay") ||
                    lower.contains("shop") || lower.contains("buy") ||
                    lower.contains("store") || lower.contains("mall") ||
                    lower.contains("daraz") || lower.contains("alibaba") ||
                    lower.contains("aliexpress") || lower.contains("temu") ||
                    lower.contains("noon") || lower.contains("flipkart") ||
                    lower.contains("shein") || lower.contains("walmart") ->
                AppCategory.SHOPPING

            // 📰 News Apps
            lower.contains("news") || lower.contains("cnn") ||
                    lower.contains("bbc") || lower.contains("times") ||
                    lower.contains("guardian") || lower.contains("post") ||
                    lower.contains("tribune") || lower.contains("nytimes") ->
                AppCategory.NEWS

//            // 🌐 Browsing Apps
//            lower.contains("chrome") || lower.contains("firefox") ||
//                    lower.contains("brave") || lower.contains("opera") ||
//                    lower.contains("browser") || lower.contains("edge") ->
//                AppCategory.BROWSING

            // 💬 Messaging Apps
            lower.contains("messenger") || lower.contains("messages") ||
                    lower.contains("viber") || lower.contains("imo") ||
                    lower.contains("signal") || lower.contains("line") || lower.contains("whatsapp") ||
                    lower.contains("telegram") || lower.contains("discord") ->
                AppCategory.MESSAGING

            // ❔ Fallback
            else -> AppCategory.GENERAL
        }
    }




    enum class AppCategory {
        // ✅ Commonly Blocked Categories (Primary focus)
        SOCIAL_MEDIA,     // Instagram, TikTok, Facebook, Reddit, Snapchat
        ENTERTAINMENT,    // YouTube, Netflix, Spotify, Disney+, Twitch
        GAMING,           // PUBG, Clash of Clans, Candy Crush, Roblox
        SHOPPING,         // Amazon, Daraz, AliExpress, Temu

        // ⚠️ Sometimes Blocked (Secondary focus)
        NEWS,             // CNN, BBC, Google News
       // BROWSING,         // Chrome, Firefox, Brave
        MESSAGING,        // WhatsApp, Telegram

        // ❔ Everything else
        GENERAL
    }

    @Composable
    fun ModernBlockerOverlay(
        blockedApp: String,
        appCategory: AppCategory,
        onChooseToClose: () -> Unit
    ) {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

        // Context-aware reflective messages based on time, app category, and user behavior
        val contextualMessages = getContextualMessages(currentHour, isWeekend, appCategory)

        val currentMessage = remember { contextualMessages.random() }
        var showContent by remember { mutableStateOf(false) }
        var buttonClicked by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(500)
            showContent = true
        }

        Dialog(
            onDismissRequest = { }, // Don't dismiss on outside click
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1A1A2E), // Deep dark blue
                                Color(0xFF16213E), // Medium dark blue
                                Color(0xFF0F3460)  // Darker blue
                            ),
                            center = Offset(0.3f, 0.1f),
                            radius = 1000f
                        )
                    )
                    .blur(if (showContent) 0.dp else 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Animated background particles/elements
                AnimatedBackgroundElements()

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(1000)) + scaleIn(
                        initialScale = 0.85f,
                        animationSpec = tween(1000, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(animationSpec = tween(500))
                ) {
                    ReflectiveCard(
                        message = currentMessage,
                        onChooseToClose = {
                            buttonClicked = true
                            onChooseToClose()
                        },
                        buttonClicked = buttonClicked
                    )
                }
            }
        }
    }

    @Composable
    fun AnimatedBackgroundElements() {
        val infiniteTransition = rememberInfiniteTransition(label = "background_animation")

        // Floating orbs animation
        val orb1Offset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "orb1"
        )

        val orb2Offset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -80f,
            animationSpec = infiniteRepeatable(
                animation = tween(6000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "orb2"
        )

        // Background gradient orbs
        Box(
            modifier = Modifier
                .offset(x = orb1Offset.dp, y = (-200).dp)
                .size(200.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6C63FF).copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .offset(x = orb2Offset.dp, y = 250.dp)
                .size(150.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4ECDC4).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }

    @Composable
    fun ReflectiveCard(
        message: ReflectiveMessage,
        onChooseToClose: () -> Unit,
        buttonClicked: Boolean
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .drawBehind {
                    // Outer glow effect
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6C63FF).copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.maxDimension
                        )
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.4f) // Glassmorphic background
            ),
            shape = RoundedCornerShape(28.dp),
//            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.1f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .drawBehind {
                        // Glass border effect
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.3f),
                            size = size,
                            cornerRadius = CornerRadius(28.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .padding(36.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated Icon with enhanced breathing and glow effects
                    val infiniteTransition = rememberInfiniteTransition(label = "icon_animation")
                    val iconScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "icon_scale"
                    )

                    val iconGlow by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "icon_glow"
                    )

                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .graphicsLayer(
                                scaleX = iconScale,
                                scaleY = iconScale,
                                alpha = iconGlow
                            )
                            .drawBehind {
                                // Icon glow effect
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF6C63FF).copy(alpha = 0.3f * iconGlow),
                                            Color.Transparent
                                        ),
                                        radius = size.maxDimension * 0.8f
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = message.actionIcon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF6C63FF) // Primary accent color
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Main reflective question with gradient text effect
                    Text(
                        text = message.mainText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                        modifier = Modifier.drawBehind {
                            // Text shadow effect
//                            drawRect(
//                                brush = Brush.verticalGradient(
//                                    colors = listOf(
//                                        Color(0xFF6C63FF).copy(alpha = 0.1f),
//                                        Color.Transparent
//                                    )
//                                )
//                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subtitle with enhanced styling
                    Text(
                        text = message.subText,
                        fontSize = 17.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // Glassmorphic button with enhanced effects
                    Button(
                        onClick = onChooseToClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .drawBehind {
                                // Button glow effect
//                                drawRoundRect(
//                                    brush = Brush.radialGradient(
//                                        colors = listOf(
//                                            Color(0xFF6C63FF).copy(alpha = 0.3f),
//                                            Color.Transparent
//                                        ),
//                                        center = center,
//                                        radius = size.maxDimension * 0.8f
//                                    ),
//                                    cornerRadius = CornerRadius(20.dp.toPx())
//                                )
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (buttonClicked) 2.dp else 0.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF6C63FF).copy(alpha = 0.8f),
                                            Color(0xFF4ECDC4).copy(alpha = 0.6f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .drawBehind {
                                    // Glass border on button
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.3f),
                                        size = size,
                                        cornerRadius = CornerRadius(20.dp.toPx()),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = message.actionIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = message.actionText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Motivational footer with accent color
                    Text(
                        text = message.motivationalFooter,
                        fontSize = 14.sp,
                        color = Color(0xFF4ECDC4).copy(alpha = 0.9f), // Teal accent
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    @Composable
    fun getContextualMessages(
        hour: Int,
        isWeekend: Boolean,
        appCategory: AppCategory
    ): List<ReflectiveMessage> {
        val baseMessages = when (appCategory) {
            AppCategory.SOCIAL_MEDIA -> getSocialMediaMessages(hour, isWeekend)
            AppCategory.ENTERTAINMENT -> getEntertainmentMessages(hour, isWeekend)
            AppCategory.GAMING -> getGamingMessages(hour, isWeekend)
            AppCategory.SHOPPING -> getShoppingMessages(hour, isWeekend)
            AppCategory.NEWS -> getNewsMessages(hour, isWeekend)
         //   AppCategory.BROWSING -> getBrowsingMessages(hour, isWeekend)
            AppCategory.MESSAGING -> getMessagingMessages(hour, isWeekend)
            AppCategory.GENERAL -> getGeneralMessages(hour, isWeekend)
        }
        return baseMessages
    }

    @Composable
    fun getMessagingMessages(hour: Int, isWeekend: Boolean): List<ReflectiveMessage> {
        return when {
            // Early Morning (5-8 AM)
            hour in 5..8 -> listOf(
                ReflectiveMessage(
                    mainText = "Good morning, changemaker! 🌅",
                    subText = "Your words have power. What uplifting message could you send to brighten someone's entire day?",
                    actionText = "Spread Light",
                    actionIcon = Icons.Default.Send,
                    motivationalFooter = "One kind message can change someone's whole day ✨"
                ),
                ReflectiveMessage(
                    mainText = "The morning messenger awakens! 📱",
                    subText = "You're about to set the tone for every conversation today. Will your words lift people up or bring them down?",
                    actionText = "Choose Positivity",
                    actionIcon = Icons.Default.Mood,
                    motivationalFooter = "Your energy is contagious - make it count 🌞"
                ),
                ReflectiveMessage(
                    mainText = "Rise and communicate! 🗣️",
                    subText = "Before checking what others said, think: what meaningful connection could you create right now?",
                    actionText = "Create Connection",
                    actionIcon = Icons.Default.ConnectWithoutContact,
                    motivationalFooter = "Real relationships are built one message at a time 💫"
                ),
                ReflectiveMessage(
                    mainText = "Your voice matters this morning! 🎤",
                    subText = "Someone in your contact list might need encouragement today. You could be their unexpected blessing.",
                    actionText = "Be Someone's Blessing",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "Be the reason someone smiles today 😊"
                ),
                ReflectiveMessage(
                    mainText = "Morning wisdom keeper! 📚",
                    subText = "Instead of consuming endless chatter, what valuable insight or encouragement could you share?",
                    actionText = "Share Wisdom",
                    actionIcon = Icons.Default.Lightbulb,
                    motivationalFooter = "Your words today become someone's strength tomorrow 💪"
                )
            )

            // Mid Morning (9-11 AM)
            hour in 9..11 -> listOf(
                ReflectiveMessage(
                    mainText = "Peak focus time detected! 🎯",
                    subText = "Your brain is firing on all cylinders right now. Are these messages supporting your goals or scattering your focus?",
                    actionText = "Protect Your Focus",
                    actionIcon = Icons.Default.PriorityHigh,
                    motivationalFooter = "Champions guard their peak hours fiercely 🏆"
                ),
                ReflectiveMessage(
                    mainText = "Your productivity window is golden! ⏰",
                    subText = "This is when millionaires make their fortunes and artists create masterpieces. Choose wisely.",
                    actionText = "Choose Greatness",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Great things happen when you protect your prime time 🚀"
                ),
                ReflectiveMessage(
                    mainText = "Flow state guardian! 🌊",
                    subText = "You're in the zone where magic happens. Every notification is a thief trying to steal your potential.",
                    actionText = "Guard Your Magic",
                    actionIcon = Icons.Default.Shield,
                    motivationalFooter = "Your focus is your superpower - use it wisely ⚡"
                ),
                ReflectiveMessage(
                    mainText = "Morning momentum builder! 🔥",
                    subText = "Each focused hour now creates unstoppable momentum for your entire day. Don't break the chain.",
                    actionText = "Build Momentum",
                    actionIcon = Icons.Default.Speed,
                    motivationalFooter = "Momentum is the bridge between dreams and reality 🌉"
                ),
                ReflectiveMessage(
                    mainText = "Your future self is watching! 👀",
                    subText = "The choices you make in these precious morning hours will echo through your entire life.",
                    actionText = "Choose Your Legacy",
                    actionIcon = Icons.Default.History,
                    motivationalFooter = "Every great achievement started with a protected morning 🌟"
                )
            )

            // Midday (12-2 PM)
            hour in 12..14 -> listOf(
                ReflectiveMessage(
                    mainText = "Nourish your soul, not just your body! 🍽️",
                    subText = "Lunchtime is for recharging your spirit. What meaningful conversation could feed your heart?",
                    actionText = "Feed Your Heart",
                    actionIcon = Icons.Default.Groups,
                    motivationalFooter = "The best conversations happen when we're fully present 💕"
                ),
                ReflectiveMessage(
                    mainText = "Midday connection catalyst! 🤝",
                    subText = "This is the perfect time to strengthen bonds with loved ones. Your presence is the greatest gift.",
                    actionText = "Give Your Presence",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "Presence is the most precious present you can give 🎁"
                ),
                ReflectiveMessage(
                    mainText = "Energy renewal station! 🔋",
                    subText = "You're at the recharge point of your day. Choose conversations that energize, not drain you.",
                    actionText = "Choose Energy",
                    actionIcon = Icons.Default.Battery6Bar,
                    motivationalFooter = "Surround yourself with people who fuel your dreams 🔥"
                ),
                ReflectiveMessage(
                    mainText = "Relationship investment time! 💰",
                    subText = "The richest people in the world are rich in relationships. What investment will you make today?",
                    actionText = "Invest in Relationships",
                    actionIcon = Icons.Default.AccountBalance,
                    motivationalFooter = "Relationships are the only wealth that multiplies when shared 📈"
                ),
                ReflectiveMessage(
                    mainText = "Midday gratitude messenger! 🙏",
                    subText = "Take a moment to thank someone who's made a difference in your life. Gratitude creates miracles.",
                    actionText = "Express Gratitude",
                    actionIcon = Icons.Default.Star,
                    motivationalFooter = "Gratitude turns what we have into enough ✨"
                )
            )

            // Afternoon (3-5 PM)
            hour in 15..17 -> listOf(
                ReflectiveMessage(
                    mainText = "Afternoon attention architect! 🏗️",
                    subText = "You're building the foundation for tomorrow's success. Every notification is a distraction from your destiny.",
                    actionText = "Build Your Destiny",
                    actionIcon = Icons.Default.NotificationsOff,
                    motivationalFooter = "Your attention is your most valuable asset 💎"
                ),
                ReflectiveMessage(
                    mainText = "Productivity protector! 🛡️",
                    subText = "The afternoon is where dreams either die or multiply. You have the power to choose which.",
                    actionText = "Multiply Your Dreams",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Champions finish strong, especially in the afternoon 🏃‍♂️"
                ),
                ReflectiveMessage(
                    mainText = "Focus warrior! ⚔️",
                    subText = "While others get distracted, you stay disciplined. This is what separates legends from the crowd.",
                    actionText = "Stay Legendary",
                    actionIcon = Icons.Default.Sports,
                    motivationalFooter = "Discipline is the bridge between goals and accomplishment 🌉"
                ),
                ReflectiveMessage(
                    mainText = "Afternoon achievement hunter! 🎯",
                    subText = "Every great accomplishment happens when others are scrolling. What will you achieve today?",
                    actionText = "Achieve Something Great",
                    actionIcon = Icons.Default.EmojiEvents,
                    motivationalFooter = "While others scroll, legends build their empire 🏛️"
                ),
                ReflectiveMessage(
                    mainText = "Your standards define you! 📏",
                    subText = "High achievers have high standards for how they spend their time. What are your standards?",
                    actionText = "Raise Your Standards",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Your standards become your reality 🎯"
                )
            )

            // Evening (6-8 PM)
            hour in 18..20 -> listOf(
                ReflectiveMessage(
                    mainText = "Evening relationship cultivator! 🌺",
                    subText = "This is the golden hour for deepening connections. Your loved ones deserve your full attention.",
                    actionText = "Deepen Connections",
                    actionIcon = Icons.Default.ConnectWithoutContact,
                    motivationalFooter = "Love grows in the garden of attention 🌹"
                ),
                ReflectiveMessage(
                    mainText = "Quality time creator! ⏰",
                    subText = "You can't buy more time, but you can create more meaningful moments. What moment will you create?",
                    actionText = "Create Meaningful Moments",
                    actionIcon = Icons.Default.Schedule,
                    motivationalFooter = "Moments become memories, memories become treasures 💝"
                ),
                ReflectiveMessage(
                    mainText = "Evening energy alchemist! 🔮",
                    subText = "Transform your evening energy into something beautiful. Your presence can heal, inspire, and love.",
                    actionText = "Transform Energy",
                    actionIcon = Icons.Default.AutoFixHigh,
                    motivationalFooter = "Your energy is magic - use it to create miracles ✨"
                ),
                ReflectiveMessage(
                    mainText = "Connection architect! 🏗️",
                    subText = "Great relationships are built in moments like these. What bridge will you build tonight?",
                    actionText = "Build Bridges",
                    actionIcon = Icons.Default.Groups,
                    motivationalFooter = "Every great relationship was built one conversation at a time 🌉"
                ),
                ReflectiveMessage(
                    mainText = "Evening memory maker! 📸",
                    subText = "Tonight could be the night someone remembers forever. What memory will you help create?",
                    actionText = "Create Lasting Memories",
                    actionIcon = Icons.Default.PhotoCamera,
                    motivationalFooter = "The best memories are made when phones are forgotten 📷"
                )
            )

            // Night (9-11 PM)
            hour in 21..23 -> listOf(
                ReflectiveMessage(
                    mainText = "Evening peace guardian! 🕊️",
                    subText = "Your mind needs tranquility to process the day's experiences. Choose conversations that bring peace.",
                    actionText = "Choose Peace",
                    actionIcon = Icons.Default.Bedtime,
                    motivationalFooter = "Peaceful evenings create powerful mornings 🌙"
                ),
                ReflectiveMessage(
                    mainText = "Sleep quality protector! 😴",
                    subText = "Your tomorrow depends on tonight's rest. Every notification is stealing from your future energy.",
                    actionText = "Protect Your Tomorrow",
                    actionIcon = Icons.Default.NightlightRound,
                    motivationalFooter = "Great days are born from great nights 🌟"
                ),
                ReflectiveMessage(
                    mainText = "Mind sanctuary creator! 🧘",
                    subText = "Create a sanctuary of calm in your mind. Let go of the day's chaos and embrace serenity.",
                    actionText = "Embrace Serenity",
                    actionIcon = Icons.Default.Spa,
                    motivationalFooter = "Inner peace is the ultimate luxury 🧘‍♀️"
                ),
                ReflectiveMessage(
                    mainText = "Dream preparation specialist! 💭",
                    subText = "Your subconscious is preparing for tomorrow's miracles. Feed it with positive thoughts, not digital chaos.",
                    actionText = "Feed Your Dreams",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Dreams planted in peace bloom into reality 🌸"
                ),
                ReflectiveMessage(
                    mainText = "Tomorrow's energy bank! 🏦",
                    subText = "Every minute of quality sleep is an investment in tomorrow's potential. Choose your investment wisely.",
                    actionText = "Invest in Tomorrow",
                    actionIcon = Icons.Default.AccountBalance,
                    motivationalFooter = "Your best investment is in your own recovery 💰"
                )
            )

            // Late Night (12-4 AM)
            hour in 0..4 -> listOf(
                ReflectiveMessage(
                    mainText = "Night warrior, your body needs you! 🦸",
                    subText = "Even superheroes need rest. Your body is literally repairing itself - don't interrupt the miracle.",
                    actionText = "Let the Miracle Happen",
                    actionIcon = Icons.Default.NightlightRound,
                    motivationalFooter = "Your body is performing miracles - honor it with rest 🌙"
                ),
                ReflectiveMessage(
                    mainText = "Future self advocate! 🛡️",
                    subText = "Your morning self is counting on you to make the right choice now. Be the hero of your own story.",
                    actionText = "Be Your Own Hero",
                    actionIcon = Icons.Default.Security,
                    motivationalFooter = "Heroes make hard choices when it matters most 🦸‍♀️"
                ),
                ReflectiveMessage(
                    mainText = "Sleep debt collector! 💸",
                    subText = "Every minute awake now costs you energy, focus, and joy tomorrow. Pay yourself first with rest.",
                    actionText = "Pay Yourself First",
                    actionIcon = Icons.Default.Bedtime,
                    motivationalFooter = "Sleep is the interest on the investment of your life 🏦"
                ),
                ReflectiveMessage(
                    mainText = "Health wealth protector! 💎",
                    subText = "Your health is your greatest wealth. Nothing in this app is worth more than your wellbeing.",
                    actionText = "Protect Your Wealth",
                    actionIcon = Icons.Default.HealthAndSafety,
                    motivationalFooter = "Health is the crown only the sick can see 👑"
                ),
                ReflectiveMessage(
                    mainText = "Tomorrow's champion! 🏆",
                    subText = "Champions are made in moments like these. Choose rest and wake up ready to conquer the world.",
                    actionText = "Choose Victory",
                    actionIcon = Icons.Default.EmojiEvents,
                    motivationalFooter = "Every champion knows when to rest and when to rise 🌅"
                )
            )

            else -> getGeneralMessages(hour, isWeekend)
        }
    }

    @Composable
    fun getSocialMediaMessages(hour: Int, isWeekend: Boolean): List<ReflectiveMessage> {
        return when {
            // Early Morning (5-8 AM)
            hour in 5..8 -> listOf(
                ReflectiveMessage(
                    mainText = "Good morning, life creator! 🌅",
                    subText = "You're about to witness the sunrise of your potential. Don't let someone else's highlight reel dim your light.",
                    actionText = "Shine Your Light",
                    actionIcon = Icons.Default.WbSunny,
                    motivationalFooter = "Your journey is more beautiful than their highlights ✨"
                ),
                ReflectiveMessage(
                    mainText = "Morning reality architect! 🏗️",
                    subText = "Every scroll is a choice between building your dreams or watching others live theirs. What will you build?",
                    actionText = "Build Your Dreams",
                    actionIcon = Icons.Default.Construction,
                    motivationalFooter = "Architects of destiny don't have time for spectating 🏛️"
                ),
                ReflectiveMessage(
                    mainText = "Sunrise warrior! ⚔️",
                    subText = "While others perform for likes, you could be creating something that changes the world. Your mission awaits.",
                    actionText = "Accept Your Mission",
                    actionIcon = Icons.Default.SelfImprovement,
                    motivationalFooter = "Warriors create legends, not consume them 🗡️"
                ),
                ReflectiveMessage(
                    mainText = "Morning energy guardian! 🔥",
                    subText = "Your morning energy is liquid gold. Don't pour it into someone else's validation machine.",
                    actionText = "Guard Your Gold",
                    actionIcon = Icons.Default.Security,
                    motivationalFooter = "Your energy is currency - spend it on yourself 💰"
                ),
                ReflectiveMessage(
                    mainText = "Authentic life designer! 🎨",
                    subText = "The most beautiful life is the one you design, not the one you scroll through. Start designing.",
                    actionText = "Design Your Life",
                    actionIcon = Icons.Default.Palette,
                    motivationalFooter = "Your authentic life is your masterpiece 🖼️"
                )
            )

            // Mid Morning (9-11 AM)
            hour in 9..11 -> listOf(
                ReflectiveMessage(
                    mainText = "Peak performance protector! 🏔️",
                    subText = "You're in the zone where millionaires are made and dreams come true. Don't trade your peak for their posts.",
                    actionText = "Protect Your Peak",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Peak performers guard their power hours 🏆"
                ),
                ReflectiveMessage(
                    mainText = "Productivity powerhouse! ⚡",
                    subText = "This is when legends are built and empires are created. Your future self is begging you to choose greatness.",
                    actionText = "Choose Greatness",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Greatness is built in moments like these 🚀"
                ),
                ReflectiveMessage(
                    mainText = "Focus force field! 🛡️",
                    subText = "Your focus is your superpower. Every notification is kryptonite. Protect your strength.",
                    actionText = "Activate Your Superpower",
                    actionIcon = Icons.Default.Shield,
                    motivationalFooter = "Focus is the ultimate superpower 💪"
                ),
                ReflectiveMessage(
                    mainText = "Morning momentum master! 🌊",
                    subText = "You're riding the wave of unlimited potential. Don't let social media crash your wave.",
                    actionText = "Ride Your Wave",
                    actionIcon = Icons.Default.Speed,
                    motivationalFooter = "Momentum is magic - don't break the spell 🔮"
                ),
                ReflectiveMessage(
                    mainText = "Future fortune builder! 💎",
                    subText = "Every focused hour now could be worth millions later. What's your hour worth to you?",
                    actionText = "Invest in Your Fortune",
                    actionIcon = Icons.Default.AccountBalance,
                    motivationalFooter = "Your focused hours are your future fortune 💰"
                )
            )

            // Midday (12-2 PM)
            hour in 12..14 -> listOf(
                ReflectiveMessage(
                    mainText = "Midday mindfulness master! 🧘",
                    subText = "This is your chance to recharge your soul, not drain it with comparison. Choose nourishment.",
                    actionText = "Nourish Your Soul",
                    actionIcon = Icons.Default.Restaurant,
                    motivationalFooter = "Feed your soul, not your ego 🍽️"
                ),
                ReflectiveMessage(
                    mainText = "Energy renewal specialist! 🔋",
                    subText = "Your energy is precious fuel. Don't waste it on content that leaves you feeling empty.",
                    actionText = "Choose Fulfillment",
                    actionIcon = Icons.Default.Battery6Bar,
                    motivationalFooter = "True energy comes from meaningful activities 🌟"
                ),
                ReflectiveMessage(
                    mainText = "Afternoon preparation champion! 🏃‍♂️",
                    subText = "How you spend your lunch break determines your afternoon energy. Choose activities that energize.",
                    actionText = "Energize Your Afternoon",
                    actionIcon = Icons.Default.DirectionsRun,
                    motivationalFooter = "Smart breaks create unstoppable momentum 🚀"
                ),
                ReflectiveMessage(
                    mainText = "Real connection curator! 🤝",
                    subText = "Instead of watching others connect, create real connections. Your heart knows the difference.",
                    actionText = "Create Real Connections",
                    actionIcon = Icons.Default.Groups,
                    motivationalFooter = "Real connections heal, fake ones steal 💕"
                ),
                ReflectiveMessage(
                    mainText = "Authenticity advocate! 💯",
                    subText = "You're being called to live authentically. Every scroll away from your truth is a betrayal of your soul.",
                    actionText = "Live Authentically",
                    actionIcon = Icons.Default.Person,
                    motivationalFooter = "Authenticity is the highest form of rebellion 🔥"
                )
            )

            // Afternoon (3-5 PM)
            hour in 15..17 -> listOf(
                ReflectiveMessage(
                    mainText = "Afternoon achievement architect! 🏗️",
                    subText = "This is when champions separate from the crowd. While others scroll, you could be soaring.",
                    actionText = "Soar Above the Crowd",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Champions are made in the afternoon grind 🏆"
                ),
                ReflectiveMessage(
                    mainText = "Productivity protector! 🛡️",
                    subText = "Your afternoon energy is liquid gold. Don't pour it into someone else's validation machine.",
                    actionText = "Protect Your Gold",
                    actionIcon = Icons.Default.Security,
                    motivationalFooter = "Your energy is more precious than any content 💰"
                ),
                ReflectiveMessage(
                    mainText = "Dream defender! ⚔️",
                    subText = "Every minute spent on social media is a minute stolen from your dreams. Defend your destiny.",
                    actionText = "Defend Your Dreams",
                    actionIcon = Icons.Default.Shield,
                    motivationalFooter = "Dreams don't work unless you do 💪"
                ),
                ReflectiveMessage(
                    mainText = "Personal growth gardener! 🌱",
                    subText = "While others consume content, you could be growing. What seeds will you plant in your mind today?",
                    actionText = "Plant Growth Seeds",
                    actionIcon = Icons.Default.Spa,
                    motivationalFooter = "Growth happens when you choose development over distraction 🌿"
                ),
                ReflectiveMessage(
                    mainText = "Legacy builder! 🏛️",
                    subText = "Every great legacy was built one focused afternoon at a time. What will your legacy be?",
                    actionText = "Build Your Legacy",
                    actionIcon = Icons.Default.AccountBalance,
                    motivationalFooter = "Legends are built in moments like these 👑"
                )
            )

            // Evening (6-8 PM)
            hour in 18..20 -> listOf(
                ReflectiveMessage(
                    mainText = "Evening connection conductor! 🎼",
                    subText = "This is the time to orchestrate beautiful moments with people who matter. Be the conductor of connection.",
                    actionText = "Conduct Beautiful Moments",
                    actionIcon = Icons.Default.Groups,
                    motivationalFooter = "The best content is the moment you're living 🎭"
                ),
                ReflectiveMessage(
                    mainText = "Quality time creator! ⏰",
                    subText = "You can't buy more time, but you can create more meaningful moments. What moment will you create?",
                    actionText = "Create Meaningful Moments",
                    actionIcon = Icons.Default.Schedule,
                    motivationalFooter = "Presence is the greatest present 🎁"
                ),
                ReflectiveMessage(
                    mainText = "Love amplifier! 💕",
                    subText = "Your presence has the power to heal, inspire, and transform. Use this power wisely.",
                    actionText = "Amplify Love",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "Love grows when you're fully present 🌹"
                ),
                ReflectiveMessage(
                    mainText = "Memory maker! 📸",
                    subText = "The best memories are made when phones are forgotten. What memory will you create tonight?",
                    actionText = "Make Memories",
                    actionIcon = Icons.Default.PhotoCamera,
                    motivationalFooter = "Life is happening now, not on social media 🌟"
                ),
                ReflectiveMessage(
                    mainText = "Evening energy alchemist! 🔮",
                    subText = "Transform your evening energy into something magical. Your presence can create miracles.",
                    actionText = "Create Miracles",
                    actionIcon = Icons.Default.AutoFixHigh,
                    motivationalFooter = "Your energy is magic - use it to create wonder ✨"
                )
            )

            // Night (9-11 PM)
            hour in 21..23 -> listOf(
                ReflectiveMessage(
                    mainText = "Peace guardian! 🕊️",
                    subText = "Your mind needs sanctuary from the chaos. Social media at night is poison for your peace.",
                    actionText = "Guard Your Peace",
                    actionIcon = Icons.Default.Shield,
                    motivationalFooter = "Peace is the foundation of all success 🧘‍♀️"
                ),
                ReflectiveMessage(
                    mainText = "Sleep quality protector! 😴",
                    subText = "Every scroll now steals from tomorrow's energy. Your future self is begging you to choose rest.",
                    actionText = "Choose Rest",
                    actionIcon = Icons.Default.Bedtime,
                    motivationalFooter = "Great days are born from great nights 🌙"
                ),
                ReflectiveMessage(
                    mainText = "Mental health advocate! 🧠",
                    subText = "Late night social media is a direct attack on your mental health. You deserve better.",
                    actionText = "Choose Mental Health",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Your mental health is your greatest wealth 💎"
                ),
                ReflectiveMessage(
                    mainText = "Tomorrow's champion! 🏆",
                    subText = "Champions are made in moments like these. Choose rest and wake up ready to conquer.",
                    actionText = "Choose Victory",
                    actionIcon = Icons.Default.EmojiEvents,
                    motivationalFooter = "Every champion knows when to rest 👑"
                ),
                ReflectiveMessage(
                    mainText = "Dream protector! 💭",
                    subText = "Your dreams are precious seeds. Don't let social media's weeds choke them out.",
                    actionText = "Protect Your Dreams",
                    actionIcon = Icons.Default.Spa,
                    motivationalFooter = "Dreams need peaceful soil to grow 🌱"
                )
            )

            // Late Night (12-4 AM)
            hour in 0..4 -> listOf(
                ReflectiveMessage(
                    mainText = "Night warrior, your body needs you! 🦸",
                    subText = "Your body is performing miracles right now - healing, growing, preparing. Don't interrupt the magic.",
                    actionText = "Honor the Magic",
                    actionIcon = Icons.Default.NightlightRound,
                    motivationalFooter = "Your body is a temple - treat it with reverence 🏛️"
                ),
                ReflectiveMessage(
                    mainText = "Future self advocate! 🛡️",
                    subText = "Your morning self is counting on you to make the right choice now. Be the hero of your own story.",
                    actionText = "Be Your Own Hero",
                    actionIcon = Icons.Default.Security,
                    motivationalFooter = "Heroes make hard choices when others don't 🦸‍♀️"
                ),
                ReflectiveMessage(
                    mainText = "Health wealth protector! 💎",
                    subText = "Your health is your greatest wealth. Nothing on social media is worth more than your wellbeing.",
                    actionText = "Protect Your Wealth",
                    actionIcon = Icons.Default.HealthAndSafety,
                    motivationalFooter = "Health is the crown only the sick can see 👑"
                ),
                ReflectiveMessage(
                    mainText = "Life force guardian! 🔋",
                    subText = "Your life force is sacred energy. Don't drain it on content that leaves you feeling empty.",
                    actionText = "Guard Your Life Force",
                    actionIcon = Icons.Default.Battery6Bar,
                    motivationalFooter = "Your life force is your most precious resource ⚡"
                ),
                ReflectiveMessage(
                    mainText = "Tomorrow's miracle maker! ✨",
                    subText = "Every minute of rest now creates tomorrow's miracles. Choose to be a miracle maker.",
                    actionText = "Make Tomorrow's Miracles",
                    actionIcon = Icons.Default.AutoFixHigh,
                    motivationalFooter = "Miracles are born from moments of surrender 🌟"
                )
            )

            else -> getGeneralMessages(hour, isWeekend)
        }
    }

    @Composable
    fun getEntertainmentMessages(hour: Int, isWeekend: Boolean): List<ReflectiveMessage> {
        return when {
            // Early Morning (5-8 AM)
            hour in 5..8 -> listOf(
                ReflectiveMessage(
                    mainText = "The sunrise whispers of new beginnings.",
                    subText = "While the world sleeps, you have the rare gift of quiet morning hours. What legacy will you build with this precious time?",
                    actionText = "Build Your Legacy",
                    actionIcon = Icons.Default.WbSunny,
                    motivationalFooter = "Champions are made in the quiet hours when others sleep 🌅"
                ),
                ReflectiveMessage(
                    mainText = "Your morning mind is a superpower.",
                    subText = "Right now, your brain is operating at peak performance. Will you use this gift to consume or create something extraordinary?",
                    actionText = "Create Something Extraordinary",
                    actionIcon = Icons.Default.AutoAwesome,
                    motivationalFooter = "The most successful people protect their morning energy 🧠✨"
                ),
                ReflectiveMessage(
                    mainText = "Every morning is a fresh canvas.",
                    subText = "You can paint today's story with purpose and intention, or let it fade into background noise. What masterpiece will you create?",
                    actionText = "Paint Your Masterpiece",
                    actionIcon = Icons.Default.Palette,
                    motivationalFooter = "Artists don't wake up to consume art—they wake up to create it 🎨"
                ),
                ReflectiveMessage(
                    mainText = "The early bird doesn't just catch the worm.",
                    subText = "You're already ahead of 90% of people by being awake. Why not stay ahead by doing something that moves you forward?",
                    actionText = "Stay Ahead",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Success is built one morning at a time 🚀"
                ),
                ReflectiveMessage(
                    mainText = "Your future self is watching.",
                    subText = "One year from now, will you thank yourself for how you spent these precious morning minutes? Make a choice that creates pride, not regret.",
                    actionText = "Make Your Future Self Proud",
                    actionIcon = Icons.Default.EmojiEvents,
                    motivationalFooter = "Every moment is an investment in your future 🏆"
                )
            )

            // Mid Morning (9-11 AM)
            hour in 9..11 -> listOf(
                ReflectiveMessage(
                    mainText = "This is your golden hour for achievement.",
                    subText = "Science proves it: your brain is at its cognitive peak right now. Are you using this neurological advantage to change your life?",
                    actionText = "Leverage Your Peak Brain",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Peak performance happens in peak hours 🧠💪"
                ),
                ReflectiveMessage(
                    mainText = "While others scroll, you could soar.",
                    subText = "Most people waste their most productive hours on entertainment. You have the power to be different—to be exceptional.",
                    actionText = "Choose to Be Exceptional",
                    actionIcon = Icons.Default.Flight,
                    motivationalFooter = "Extraordinary people do ordinary things extraordinarily well 🦅"
                ),
                ReflectiveMessage(
                    mainText = "Your dreams are waiting for you.",
                    subText = "Every skill you want to master, every goal you want to achieve—they're all possible. But only if you choose progress over entertainment.",
                    actionText = "Chase Your Dreams",
                    actionIcon = Icons.Default.StarOutline,
                    motivationalFooter = "Dreams don't work unless you do ⭐"
                ),
                ReflectiveMessage(
                    mainText = "The compound effect starts now.",
                    subText = "Small actions taken consistently in these prime hours create massive results. What small step will you take toward your biggest goal?",
                    actionText = "Take Your Small Step",
                    actionIcon = Icons.Default.DirectionsWalk,
                    motivationalFooter = "Success is the sum of small efforts repeated daily 📈"
                ),
                ReflectiveMessage(
                    mainText = "Your competition is working right now.",
                    subText = "While you're here, someone else is building skills, creating value, and moving closer to their goals. Will you join them or fall behind?",
                    actionText = "Join the Builders",
                    actionIcon = Icons.Default.Build,
                    motivationalFooter = "Champions train while others are entertained 🏃‍♂️"
                )
            )

            // Midday (12-2 PM)
            hour in 12..14 -> listOf(
                ReflectiveMessage(
                    mainText = "Your energy is a precious resource.",
                    subText = "You wouldn't pour premium gasoline into a broken engine. Why pour your precious midday energy into mindless consumption?",
                    actionText = "Invest Your Energy Wisely",
                    actionIcon = Icons.Default.Battery4Bar,
                    motivationalFooter = "Energy invested in growth compounds infinitely ⚡"
                ),
                ReflectiveMessage(
                    mainText = "The afternoon is your second chance.",
                    subText = "If your morning wasn't perfect, this moment offers redemption. What would make you feel unstoppable by evening?",
                    actionText = "Seize Your Redemption",
                    actionIcon = Icons.Default.RestartAlt,
                    motivationalFooter = "Every moment is a chance to begin again 🔄"
                ),
                ReflectiveMessage(
                    mainText = "Your soul craves real nourishment.",
                    subText = "Entertainment is junk food for your spirit. What would truly feed your soul and leave you feeling energized and alive?",
                    actionText = "Feed Your Soul",
                    actionIcon = Icons.Default.Spa,
                    motivationalFooter = "True fulfillment comes from meaningful action 🌱"
                ),
                ReflectiveMessage(
                    mainText = "You're one decision away from transformation.",
                    subText = "This single moment could be the turning point where you choose growth over comfort. What will you choose?",
                    actionText = "Choose Transformation",
                    actionIcon = Icons.Default.Transform,
                    motivationalFooter = "Transformation begins with a single brave choice 🦋"
                ),
                ReflectiveMessage(
                    mainText = "Your body is sending you a message.",
                    subText = "That restless feeling? It's your inner wisdom saying you're meant for more than passive consumption. Listen to it.",
                    actionText = "Listen to Your Wisdom",
                    actionIcon = Icons.Default.Hearing,
                    motivationalFooter = "Your intuition knows what your mind hasn't figured out yet 🔮"
                )
            )

            // Afternoon (3-5 PM)
            hour in 15..17 -> listOf(
                ReflectiveMessage(
                    mainText = "The afternoon slump is a test.",
                    subText = "Will you take the easy path of distraction, or will you push through and emerge stronger? Your character is being forged right now.",
                    actionText = "Forge Your Character",
                    actionIcon = Icons.Default.FitnessCenter,
                    motivationalFooter = "Character is built in moments of resistance 💪"
                ),
                ReflectiveMessage(
                    mainText = "Your willpower is a muscle.",
                    subText = "Every time you choose purpose over entertainment, you're strengthening the muscle that will carry you to greatness.",
                    actionText = "Strengthen Your Will",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Willpower grows with every conscious choice 🏋️‍♂️"
                ),
                ReflectiveMessage(
                    mainText = "The world needs what you have to offer.",
                    subText = "Inside you are solutions to problems, creativity waiting to emerge, and gifts meant to be shared. Don't let them die from neglect.",
                    actionText = "Share Your Gifts",
                    actionIcon = Icons.Default.Redeem,
                    motivationalFooter = "Your unique gifts are needed in this world 🎁"
                ),
                ReflectiveMessage(
                    mainText = "This is your hero's journey moment.",
                    subText = "Every hero faces the choice between comfort and growth. Will you answer the call to adventure or stay in the ordinary world?",
                    actionText = "Answer the Call",
                    actionIcon = Icons.Default.Shield,
                    motivationalFooter = "Heroes are made in moments of choice 🦸‍♂️"
                ),
                ReflectiveMessage(
                    mainText = "Your future is being written now.",
                    subText = "The person you'll become is determined by the choices you make in moments like this. Who are you becoming?",
                    actionText = "Become Who You're Meant to Be",
                    actionIcon = Icons.Default.Person,
                    motivationalFooter = "You are always becoming—choose wisely 🌟"
                )
            )

            // Evening (6-8 PM)
            hour in 18..20 -> listOf(
                ReflectiveMessage(
                    mainText = "The evening is sacred time.",
                    subText = if (isWeekend) "Weekend evenings are rare gifts. How will you honor this precious time—with mindless consumption or meaningful connection?"
                    else "Work is done. This is your time to nurture your soul, connect with loved ones, and become more yourself.",
                    actionText = "Honor This Sacred Time",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "Sacred time deserves sacred choices 🕯️"
                ),
                ReflectiveMessage(
                    mainText = "Your relationships are your true wealth.",
                    subText = "No show, no entertainment can compare to the richness of genuine human connection. Who in your life would treasure your presence?",
                    actionText = "Treasure Your Relationships",
                    actionIcon = Icons.Default.Groups,
                    motivationalFooter = "Love is the only currency that multiplies when spent 💕"
                ),
                ReflectiveMessage(
                    mainText = "You're designing tomorrow's energy.",
                    subText = "How you spend these evening hours determines how you'll feel tomorrow. Are you setting yourself up for success or struggle?",
                    actionText = "Design Tomorrow's Success",
                    actionIcon = Icons.Default.Architecture,
                    motivationalFooter = "Tomorrow's energy is tonight's investment 🌅"
                ),
                ReflectiveMessage(
                    mainText = "Your peace is worth protecting.",
                    subText = "In a world full of noise, your inner peace is precious. What would nurture your soul and leave you feeling centered?",
                    actionText = "Protect Your Peace",
                    actionIcon = Icons.Default.Shield,
                    motivationalFooter = "Inner peace is the ultimate luxury 🧘‍♂️"
                ),
                ReflectiveMessage(
                    mainText = "This is your creativity hour.",
                    subText = "As the day winds down, your creative mind awakens. What beautiful thing could you create instead of consuming?",
                    actionText = "Create Something Beautiful",
                    actionIcon = Icons.Default.Create,
                    motivationalFooter = "Creation is the highest form of self-expression 🎨"
                )
            )

            // Night (9-11 PM)
            hour in 21..23 -> listOf(
                ReflectiveMessage(
                    mainText = "Your mind needs sanctuary.",
                    subText = "After a full day, your mind craves peace, not stimulation. What would create true sanctuary for your thoughts?",
                    actionText = "Create Mental Sanctuary",
                    actionIcon = Icons.Default.Spa,
                    motivationalFooter = "A peaceful mind is a powerful mind 🧘‍♀️"
                ),
                ReflectiveMessage(
                    mainText = "Tomorrow's version of you is being born.",
                    subText = "Your sleep quality determines your tomorrow's potential. Are you giving your future self the gift of restoration?",
                    actionText = "Gift Your Future Self",
                    actionIcon = Icons.Default.Bedtime,
                    motivationalFooter = "Great tomorrows are born from restful nights 😴"
                ),
                ReflectiveMessage(
                    mainText = "Your dreams are calling.",
                    subText = "Not the entertainment on your screen, but the dreams in your heart. What would prepare you to wake up ready to chase them?",
                    actionText = "Prepare for Your Dreams",
                    actionIcon = Icons.Default.CloudQueue,
                    motivationalFooter = "Dreams require rest to become reality 💭"
                ),
                ReflectiveMessage(
                    mainText = "This is your wisdom hour.",
                    subText = "The quiet night is perfect for reflection and gratitude. What wisdom has today taught you about yourself?",
                    actionText = "Embrace Your Wisdom",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Wisdom grows in quiet moments 🦉"
                ),
                ReflectiveMessage(
                    mainText = "Your body is your temple.",
                    subText = "Blue light and overstimulation are toxins for your temple. What would honor and heal your body instead?",
                    actionText = "Honor Your Temple",
                    actionIcon = Icons.Default.HealthAndSafety,
                    motivationalFooter = "Your body is the only home you'll ever have 🏛️"
                )
            )

            // Late Night (12-4 AM)
            hour in 0..4 -> listOf(
                ReflectiveMessage(
                    mainText = "Your future self is pleading with you.",
                    subText = "Tomorrow's you will either thank you for this choice or regret it. Listen to that voice of wisdom—choose rest.",
                    actionText = "Listen to Future You",
                    actionIcon = Icons.Default.Schedule,
                    motivationalFooter = "The best decisions are made thinking of tomorrow 🔮"
                ),
                ReflectiveMessage(
                    mainText = "This is a moment of truth.",
                    subText = "Late-night hours reveal who you really are. Are you someone who chooses long-term wellbeing over short-term entertainment?",
                    actionText = "Choose Long-term Wellbeing",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "True strength is choosing what's best for you 💪"
                ),
                ReflectiveMessage(
                    mainText = "Your health is your foundation.",
                    subText = "Every dream, every goal, every aspiration depends on your health. Protect it like the treasure it is.",
                    actionText = "Protect Your Foundation",
                    actionIcon = Icons.Default.HealthAndSafety,
                    motivationalFooter = "Health is wealth beyond measure 💎"
                ),
                ReflectiveMessage(
                    mainText = "The darkness is for healing.",
                    subText = "Your body and mind regenerate in darkness and rest. Don't rob yourself of this natural healing process.",
                    actionText = "Allow Natural Healing",
                    actionIcon = Icons.Default.Healing,
                    motivationalFooter = "Healing happens in rest and darkness 🌙"
                ),
                ReflectiveMessage(
                    mainText = "Love yourself enough to rest.",
                    subText = "Self-love isn't just about feeling good—it's about making choices that honor your deepest needs. Rest is an act of self-love.",
                    actionText = "Choose Self-Love",
                    actionIcon = Icons.Default.SelfImprovement,
                    motivationalFooter = "Self-love is the foundation of all success 💝"
                )
            )

            else -> getGeneralMessages(hour, isWeekend)
        }
    }

    @Composable
    fun getGamingMessages(hour: Int, isWeekend: Boolean): List<ReflectiveMessage> {
        return when {
            // Early Morning (5-8 AM)
            hour in 5..8 -> listOf(
                ReflectiveMessage(
                    mainText = "Your morning mind is a weapon of creation.",
                    subText = "While others sleep, you have the opportunity to build something real. Will you use this gift to escape reality or enhance it?",
                    actionText = "Enhance Your Reality",
                    actionIcon = Icons.Default.WbSunny,
                    motivationalFooter = "Real life is the ultimate open-world game 🌍"
                ),
                ReflectiveMessage(
                    mainText = "You're already winning the game of life.",
                    subText = "By being awake and alert, you're ahead of millions. Why not level up in reality where the rewards are permanent?",
                    actionText = "Level Up in Reality",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Real-world achievements unlock infinite possibilities 🚀"
                ),
                ReflectiveMessage(
                    mainText = "Your strategic mind is calling.",
                    subText = "You have the brain of a master strategist. What if you applied that same tactical thinking to conquering your real goals?",
                    actionText = "Conquer Your Real Goals",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "The greatest game is the one you're already playing—life 🎯"
                ),
                ReflectiveMessage(
                    mainText = "Every morning is a new game session.",
                    subText = "But this isn't a game you can reset. The choices you make now create permanent upgrades to your character.",
                    actionText = "Upgrade Your Character",
                    actionIcon = Icons.Default.Person,
                    motivationalFooter = "Your real character stats matter most 📊"
                ),
                ReflectiveMessage(
                    mainText = "The ultimate boss battle is with yourself.",
                    subText = "Will you let your lesser self win by choosing easy entertainment, or will your higher self triumph by choosing growth?",
                    actionText = "Let Your Higher Self Win",
                    actionIcon = Icons.Default.EmojiEvents,
                    motivationalFooter = "The greatest victory is victory over yourself 👑"
                )
            )

            // Mid Morning (9-11 AM)
            hour in 9..11 -> listOf(
                ReflectiveMessage(
                    mainText = "Your peak performance window is open.",
                    subText = "Professional gamers train during their peak hours. Are you training for success in the game that matters most—your life?",
                    actionText = "Train for Life Success",
                    actionIcon = Icons.Default.FitnessCenter,
                    motivationalFooter = "Champions train when they don't feel like it 🏆"
                ),
                ReflectiveMessage(
                    mainText = "You're built for greatness, not grinding.",
                    subText = "Your mind craves challenge and growth. Give it real problems to solve, not artificial ones designed to waste your time.",
                    actionText = "Solve Real Problems",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Your brain is too powerful for mindless grinding 🧠"
                ),
                ReflectiveMessage(
                    mainText = "The high score that matters is your life score.",
                    subText = "How many skills have you mastered? How many people have you helped? How many dreams have you achieved?",
                    actionText = "Increase Your Life Score",
                    actionIcon = Icons.Default.Score,
                    motivationalFooter = "Life achievements unlock infinite rewards 🎖️"
                ),
                ReflectiveMessage(
                    mainText = "Your competition is getting stronger.",
                    subText = "While you're here, someone else is building skills, creating value, and leveling up in reality. Will you compete or concede?",
                    actionText = "Compete in Reality",
                    actionIcon = Icons.Default.Speed,
                    motivationalFooter = "The real leaderboard is life itself 🥇"
                ),
                ReflectiveMessage(
                    mainText = "Every moment is experience points.",
                    subText = "But only if you're actively engaging with life. Passive entertainment gives zero XP toward your real character development.",
                    actionText = "Earn Real XP",
                    actionIcon = Icons.Default.Star,
                    motivationalFooter = "Real experience points compound forever ⭐"
                )
            )

            // Midday (12-2 PM)
            hour in 12..14 -> listOf(
                ReflectiveMessage(
                    mainText = "Your energy bar is depleting.",
                    subText = "In games, you manage resources carefully. Are you managing your real-life energy with the same strategic thinking?",
                    actionText = "Manage Your Energy Strategically",
                    actionIcon = Icons.Default.Battery4Bar,
                    motivationalFooter = "Energy management is the ultimate life skill ⚡"
                ),
                ReflectiveMessage(
                    mainText = "This is your respawn moment.",
                    subText = "Instead of escaping to a virtual world, what if you used this time to recharge for the real world adventure?",
                    actionText = "Recharge for Real Adventure",
                    actionIcon = Icons.Default.RestartAlt,
                    motivationalFooter = "The greatest adventure is your own life 🗺️"
                ),
                ReflectiveMessage(
                    mainText = "Your inventory is full of unused potential.",
                    subText = "You have skills, talents, and abilities waiting to be deployed. When will you equip them in the real world?",
                    actionText = "Equip Your Real Skills",
                    actionIcon = Icons.Default.Inventory,
                    motivationalFooter = "Your real-world inventory is limitless 🎒"
                ),
                ReflectiveMessage(
                    mainText = "The NPCs in your life need you.",
                    subText = "But they're not NPCs—they're real people with real needs. What side quest could you complete to help someone today?",
                    actionText = "Complete a Real Side Quest",
                    actionIcon = Icons.Default.Groups,
                    motivationalFooter = "Helping others is the ultimate multiplayer experience 🤝"
                ),
                ReflectiveMessage(
                    mainText = "Your save file is your legacy.",
                    subText = "Every choice you make is permanently saved. Are you creating a save file you'll be proud to show your future self?",
                    actionText = "Create a Proud Legacy",
                    actionIcon = Icons.Default.Save,
                    motivationalFooter = "Your legacy is written in every choice 📝"
                )
            )

            // Afternoon (3-5 PM)
            hour in 15..17 -> listOf(
                ReflectiveMessage(
                    mainText = "The afternoon boss battle is with procrastination.",
                    subText = "This is the hardest boss you'll face. But defeating it unlocks the most valuable rewards—real progress and self-respect.",
                    actionText = "Defeat Procrastination",
                    actionIcon = Icons.Default.Shield,
                    motivationalFooter = "The hardest battles yield the greatest rewards ⚔️"
                ),
                ReflectiveMessage(
                    mainText = "Your character is being tested.",
                    subText = "Will you take the easy path of instant gratification, or will you choose the harder path that leads to real character growth?",
                    actionText = "Choose Character Growth",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Character growth is the ultimate upgrade 📈"
                ),
                ReflectiveMessage(
                    mainText = "The real world has infinite possibilities.",
                    subText = "Unlike games with predetermined outcomes, your life story can go anywhere. What new chapter will you write?",
                    actionText = "Write Your New Chapter",
                    actionIcon = Icons.Default.MenuBook,
                    motivationalFooter = "Your life story has infinite possible endings 📚"
                ),
                ReflectiveMessage(
                    mainText = "Your power-up is waiting.",
                    subText = "But it's not in a game—it's in taking real action. What one thing could you do right now to feel more powerful?",
                    actionText = "Claim Your Power-Up",
                    actionIcon = Icons.Default.Bolt,
                    motivationalFooter = "Real power comes from real action ⚡"
                ),
                ReflectiveMessage(
                    mainText = "The achievement you're seeking is self-respect.",
                    subText = "No gaming achievement can give you the deep satisfaction of knowing you chose growth over comfort.",
                    actionText = "Earn Your Self-Respect",
                    actionIcon = Icons.Default.EmojiEvents,
                    motivationalFooter = "Self-respect is the rarest achievement 🏆"
                )
            )

            // Evening (6-8 PM)
            hour in 18..20 -> listOf(
                ReflectiveMessage(
                    mainText = "Your guild members are waiting.",
                    subText = if (isWeekend) "But your real guild—family and friends—offer experiences no game can match. When will you join them?"
                    else "Work is done. Your real-world guild needs you more than any online team ever could.",
                    actionText = "Join Your Real Guild",
                    actionIcon = Icons.Default.Groups,
                    motivationalFooter = "The best co-op experiences happen in real life 👥"
                ),
                ReflectiveMessage(
                    mainText = "This is your creative mode hour.",
                    subText = "Instead of consuming someone else's creation, what if you built something meaningful in your own life?",
                    actionText = "Build Something Meaningful",
                    actionIcon = Icons.Default.Create,
                    motivationalFooter = "Creating beats consuming every time 🏗️"
                ),
                ReflectiveMessage(
                    mainText = "Your health bar needs attention.",
                    subText = "In games, you monitor your health carefully. Are you giving your real body the same attention and care?",
                    actionText = "Care for Your Health",
                    actionIcon = Icons.Default.HealthAndSafety,
                    motivationalFooter = "Your health is your most important stat 💪"
                ),
                ReflectiveMessage(
                    mainText = "The evening quest is connection.",
                    subText = "Your mission, should you choose to accept it, is to connect meaningfully with the people who matter most.",
                    actionText = "Accept the Connection Quest",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "Connection is the ultimate end-game content ❤️"
                ),
                ReflectiveMessage(
                    mainText = "Your true calling is calling.",
                    subText = "Beneath the need for entertainment lies a deeper need—to express your unique gifts. What wants to be expressed through you?",
                    actionText = "Express Your Gifts",
                    actionIcon = Icons.Default.RecordVoiceOver,
                    motivationalFooter = "Your unique gifts are needed in this world 🎁"
                )
            )

            // Night (9-11 PM)
            hour in 21..23 -> listOf(
                ReflectiveMessage(
                    mainText = "Your sleep cycle is more important than any game cycle.",
                    subText = "Professional athletes prioritize sleep over entertainment. Are you treating your body like the high-performance machine it is?",
                    actionText = "Treat Yourself Like a Pro",
                    actionIcon = Icons.Default.Bedtime,
                    motivationalFooter = "Champions prioritize recovery over entertainment 🏃‍♂️"
                ),
                ReflectiveMessage(
                    mainText = "The night mode should be rest mode.",
                    subText = "Your brain needs downtime to process, heal, and prepare for tomorrow's challenges. Give it the gift of peace.",
                    actionText = "Gift Your Brain Peace",
                    actionIcon = Icons.Default.NightlightRound,
                    motivationalFooter = "A rested mind is an unstoppable mind 🧠"
                ),
                ReflectiveMessage(
                    mainText = "Your tomorrow's performance depends on tonight's choices.",
                    subText = "Every pro gamer knows that peak performance requires proper rest. Are you setting yourself up for tomorrow's wins?",
                    actionText = "Set Up Tomorrow's Wins",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Tomorrow's victories start with tonight's wisdom 🌅"
                ),
                ReflectiveMessage(
                    mainText = "The blue light is weakening your final boss.",
                    subText = "Your final boss is your best self, and blue light is literally weakening your ability to become that person.",
                    actionText = "Strengthen Your Final Boss",
                    actionIcon = Icons.Default.Shield,
                    motivationalFooter = "Your best self needs your protection 🛡️"
                ),
                ReflectiveMessage(
                    mainText = "This is your mindfulness mini-game.",
                    subText = "Instead of endless gaming, play the mini-game of gratitude. What three things made today worth playing?",
                    actionText = "Play the Gratitude Game",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "Gratitude is the ultimate cheat code for happiness 💕"
                )
            )

            // Late Night (12-4 AM)
            hour in 0..4 -> listOf(
                ReflectiveMessage(
                    mainText = "Game over for tonight.",
                    subText = "The most successful players know when to stop. Your body and mind are sending you the 'low health' warning—listen to them.",
                    actionText = "Listen to Your Body",
                    actionIcon = Icons.Default.HealthAndSafety,
                    motivationalFooter = "The wisest players know when to quit 🎮"
                ),
                ReflectiveMessage(
                    mainText = "Your real-world performance is suffering.",
                    subText = "Late-night gaming creates a cascade of poor performance in the game that matters most—your actual life.",
                    actionText = "Protect Your Real Performance",
                    actionIcon = Icons.Default.TrendingDown,
                    motivationalFooter = "Real-world performance is the only score that counts 📊"
                ),
                ReflectiveMessage(
                    mainText = "The overnight regeneration quest is calling.",
                    subText = "Your body has an automatic healing quest that only activates during sleep. Don't cancel this critical mission.",
                    actionText = "Begin Regeneration Quest",
                    actionIcon = Icons.Default.Healing,
                    motivationalFooter = "Sleep is the ultimate healing spell 🌙"
                ),
                ReflectiveMessage(
                    mainText = "Your addiction meter is in the red zone.",
                    subText = "If you're gaming at this hour, it's not entertainment anymore—it's addiction. You have the power to break free.",
                    actionText = "Break Free",
                    actionIcon = Icons.Default.LockOpen,
                    motivationalFooter = "Freedom is the ultimate achievement 🗝️"
                ),
                ReflectiveMessage(
                    mainText = "The secret level is self-control.",
                    subText = "Few players ever discover this hidden level. Those who do unlock unlimited potential in every area of life.",
                    actionText = "Unlock the Secret Level",
                    actionIcon = Icons.Default.LockOpen,
                    motivationalFooter = "Self-control is the ultimate superpower 🦸‍♂️"
                )
            )

            else -> getGeneralMessages(hour, isWeekend)
        }
    }

    @Composable
    fun getShoppingMessages(hour: Int, isWeekend: Boolean): List<ReflectiveMessage> {
        return when {
            // Early Morning (5-8 AM)
            hour in 5..8 -> listOf(
                ReflectiveMessage(
                    mainText = "The sunrise doesn't shop. It creates.",
                    subText = "Your morning energy is pure potential. Channel it into building something beautiful instead of buying something temporary.",
                    actionText = "Create Your Morning",
                    actionIcon = Icons.Default.WbSunny,
                    motivationalFooter = "Champions start their day with intention, not transactions ☀️"
                ),
                ReflectiveMessage(
                    mainText = "What if this urge is actually inspiration in disguise?",
                    subText = "That desire to acquire could be your soul asking you to create, achieve, or connect. What's the real message?",
                    actionText = "Decode Your Desire",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Your deepest urges often point to your highest purpose 🎯"
                ),
                ReflectiveMessage(
                    mainText = "Every millionaire started their day differently.",
                    subText = "While others scroll and spend, you could be learning, creating, or building. What legacy begins with this choice?",
                    actionText = "Choose Legacy",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Wealth is built in the quiet morning hours 💰"
                ),
                ReflectiveMessage(
                    mainText = "Your future self is watching this moment.",
                    subText = "In 5 years, will you remember this purchase or the morning you chose to invest in yourself instead?",
                    actionText = "Invest in Tomorrow",
                    actionIcon = Icons.Default.Savings,
                    motivationalFooter = "The best investment is always in yourself 🌟"
                ),
                ReflectiveMessage(
                    mainText = "What if gratitude is the ultimate abundance hack?",
                    subText = "Before seeking more, could you find joy in what you already have? True wealth starts with appreciation.",
                    actionText = "Practice Abundance",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "Gratitude transforms what you have into enough 🙏"
                )
            )

            // Mid Morning (9-11 AM)
            hour in 9..11 -> listOf(
                ReflectiveMessage(
                    mainText = "This is your golden hour for greatness.",
                    subText = "Your mind is at its sharpest. CEOs and creators use this time to build empires, not fill shopping carts.",
                    actionText = "Build Your Empire",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Peak performance happens in peak hours 🏆"
                ),
                ReflectiveMessage(
                    mainText = "Every scroll is a choice. Every click is a vote.",
                    subText = "You're voting for the person you want to become. Are you choosing the consumer or the creator?",
                    actionText = "Vote for Your Future",
                    actionIcon = Icons.Default.HowToVote,
                    motivationalFooter = "Your choices today shape your tomorrow 🗳️"
                ),
                ReflectiveMessage(
                    mainText = "The world needs what you have to offer.",
                    subText = "Your unique talents and perspectives are waiting to be shared. Don't let consumption steal your contribution.",
                    actionText = "Share Your Gifts",
                    actionIcon = Icons.Default.CardGiftcard,
                    motivationalFooter = "Your contribution matters more than your consumption 🎁"
                ),
                ReflectiveMessage(
                    mainText = "Successful people guard their attention like treasure.",
                    subText = "Your focus is your most valuable asset. Are you investing it wisely or spending it carelessly?",
                    actionText = "Guard Your Focus",
                    actionIcon = Icons.Default.Security,
                    motivationalFooter = "Where attention goes, energy flows 🔒"
                ),
                ReflectiveMessage(
                    mainText = "What problem could you solve instead of creating one?",
                    subText = "Every purchase creates a storage problem. Every creation solves a human problem. Which path calls to you?",
                    actionText = "Solve, Don't Shop",
                    actionIcon = Icons.Default.Lightbulb,
                    motivationalFooter = "Problem solvers change the world 💡"
                )
            )

            // Midday (12-2 PM)
            hour in 12..14 -> listOf(
                ReflectiveMessage(
                    mainText = "Your energy is sacred. How will you spend it?",
                    subText = "This moment of restlessness could fuel your next breakthrough. What if you channeled it into something meaningful?",
                    actionText = "Channel Your Energy",
                    actionIcon = Icons.Default.BatteryChargingFull,
                    motivationalFooter = "Energy invested wisely compounds infinitely ⚡"
                ),
                ReflectiveMessage(
                    mainText = "The most successful people eat lunch, not ego.",
                    subText = "While others fill emotional voids with purchases, you could nourish your body and mind. What do you really hunger for?",
                    actionText = "Nourish Yourself",
                    actionIcon = Icons.Default.Restaurant,
                    motivationalFooter = "True fulfillment comes from within 🍽️"
                ),
                ReflectiveMessage(
                    mainText = "This pause is a gift. What will you unwrap?",
                    subText = "Every moment of stillness is an opportunity for insight. What wisdom is waiting for you beyond the shopping cart?",
                    actionText = "Unwrap Wisdom",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "The best gifts can't be purchased 🎁"
                ),
                ReflectiveMessage(
                    mainText = "What if your heart is calling, not your wallet?",
                    subText = "Sometimes we shop when we need to connect. Who could you reach out to instead of reaching for your credit card?",
                    actionText = "Connect Deeply",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "Love is the ultimate luxury ❤️"
                ),
                ReflectiveMessage(
                    mainText = "Your afternoon could be legendary.",
                    subText = "History's greatest achievements happened in ordinary moments like this. What extraordinary choice will you make?",
                    actionText = "Choose Extraordinary",
                    actionIcon = Icons.Default.Star,
                    motivationalFooter = "Legends are made in unremarkable moments ⭐"
                )
            )

            // Afternoon (3-5 PM)
            hour in 15..17 -> listOf(
                ReflectiveMessage(
                    mainText = "The afternoon dip is your diamond mine.",
                    subText = "When everyone else gives in to distraction, you can choose discipline. This is where champions are forged.",
                    actionText = "Choose Champion",
                    actionIcon = Icons.Default.EmojiEvents,
                    motivationalFooter = "Diamonds are formed under pressure 💎"
                ),
                ReflectiveMessage(
                    mainText = "What if this urge is actually your intuition?",
                    subText = "Your soul knows what you need. Maybe it's not stuff - maybe it's growth, connection, or contribution.",
                    actionText = "Listen to Your Soul",
                    actionIcon = Icons.Default.Hearing,
                    motivationalFooter = "Your intuition is your inner GPS 🧭"
                ),
                ReflectiveMessage(
                    mainText = "Every master has walked this path.",
                    subText = "The urge to distract yourself is natural. But masters choose their response. What will you choose?",
                    actionText = "Choose Mastery",
                    actionIcon = Icons.Default.School,
                    motivationalFooter = "Masters are made through mindful choices 🎓"
                ),
                ReflectiveMessage(
                    mainText = "Your future is being written right now.",
                    subText = "This single choice will ripple through time. Will it be a story of impulse or intention?",
                    actionText = "Write Your Story",
                    actionIcon = Icons.Default.Edit,
                    motivationalFooter = "You are the author of your destiny 📝"
                ),
                ReflectiveMessage(
                    mainText = "What breakthrough is waiting beyond this urge?",
                    subText = "Your biggest growth often comes from not giving in to instant gratification. What could emerge from your restraint?",
                    actionText = "Embrace Growth",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Your greatest strength comes from overcoming your greatest urges 💪"
                )
            )

            // Evening (6-8 PM)
            hour in 18..20 -> listOf(
                ReflectiveMessage(
                    mainText = "Sunsets don't need upgrades. Neither do you.",
                    subText = "As the day winds down, remember that you are already complete. What would honor this truth?",
                    actionText = "Honor Your Completeness",
                    actionIcon = Icons.Default.SelfImprovement,
                    motivationalFooter = "Wholeness comes from within, not from without 🌅"
                ),
                ReflectiveMessage(
                    mainText = "The evening is for reflection, not consumption.",
                    subText = if (isWeekend) "Weekend evenings are sacred time. How will you sanctify these precious hours?"
                    else "Work is done. This is your time to be, not just to have. What would nourish your soul?",
                    actionText = "Nourish Your Soul",
                    actionIcon = Icons.Default.Spa,
                    motivationalFooter = "The soul needs presence, not presents 🧘"
                ),
                ReflectiveMessage(
                    mainText = "Your presence is the greatest present.",
                    subText = "The people who love you don't need you to buy anything. They need you to be fully here. Can you give that gift?",
                    actionText = "Give Presence",
                    actionIcon = Icons.Default.Groups,
                    motivationalFooter = "Presence is the most precious currency 👥"
                ),
                ReflectiveMessage(
                    mainText = "What if this evening became your renaissance?",
                    subText = "Great artists used evening light to create masterpieces. What masterpiece could you create with this time?",
                    actionText = "Create Your Masterpiece",
                    actionIcon = Icons.Default.Palette,
                    motivationalFooter = "Every evening is a canvas waiting for your brush 🎨"
                ),
                ReflectiveMessage(
                    mainText = "The most beautiful things in life are free.",
                    subText = "Laughter, love, sunsets, dreams - none require a transaction. What free beauty surrounds you right now?",
                    actionText = "Embrace Free Beauty",
                    actionIcon = Icons.Default.Nature,
                    motivationalFooter = "The best things in life have no price tag 🌿"
                )
            )

            // Night (9-11 PM)
            hour in 21..23 -> listOf(
                ReflectiveMessage(
                    mainText = "Night shopping is like eating in your sleep.",
                    subText = "Your conscious mind is tired, making you vulnerable to impulse. What would your well-rested self choose?",
                    actionText = "Choose for Tomorrow",
                    actionIcon = Icons.Default.Bedtime,
                    motivationalFooter = "Your morning self will thank you for this restraint 😴"
                ),
                ReflectiveMessage(
                    mainText = "The stars don't compete. They just shine.",
                    subText = "You don't need to acquire anything to be magnificent. You already are. What would honor this truth?",
                    actionText = "Honor Your Light",
                    actionIcon = Icons.Default.Star,
                    motivationalFooter = "Your light shines brightest when you're not trying to buy it ✨"
                ),
                ReflectiveMessage(
                    mainText = "What if peace is the ultimate luxury?",
                    subText = "All the things you could buy won't give you what this moment of stillness offers for free.",
                    actionText = "Choose Peace",
                    actionIcon = Icons.Default.Spa,
                    motivationalFooter = "Peace of mind is priceless 🕊️"
                ),
                ReflectiveMessage(
                    mainText = "Your dreams are calling. Will you answer?",
                    subText = "The night is for rest and dreams, not endless scrolling. What dreams deserve your attention?",
                    actionText = "Answer Your Dreams",
                    actionIcon = Icons.Default.NightlightRound,
                    motivationalFooter = "Dreams come true when you stop shopping for them 🌙"
                ),
                ReflectiveMessage(
                    mainText = "Tomorrow's success starts with tonight's choices.",
                    subText = "Every successful person has a nighttime routine that honors their future. What ritual will you create?",
                    actionText = "Create Success Rituals",
                    actionIcon = Icons.Default.Alarm,
                    motivationalFooter = "Success is a ritual, not a purchase 🏆"
                )
            )

            // Late Night (12-4 AM)
            hour in 0..4 -> listOf(
                ReflectiveMessage(
                    mainText = "Even your phone needs to sleep.",
                    subText = "This is your soul's way of saying it's time to rest. What would serve your highest good right now?",
                    actionText = "Serve Your Highest Good",
                    actionIcon = Icons.Default.NightlightRound,
                    motivationalFooter = "Your highest self needs rest more than retail 💤"
                ),
                ReflectiveMessage(
                    mainText = "The night holds wisdom, not shopping carts.",
                    subText = "Late-night impulses often lead to morning regrets. What wisdom is this moment offering you?",
                    actionText = "Embrace Wisdom",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Wisdom comes in whispers, not in purchases 🦉"
                ),
                ReflectiveMessage(
                    mainText = "Your future self is begging you to stop.",
                    subText = "Every great success story includes moments of restraint. This could be yours.",
                    actionText = "Write Your Success Story",
                    actionIcon = Icons.Default.MenuBook,
                    motivationalFooter = "Success stories are written in moments of restraint 📖"
                ),
                ReflectiveMessage(
                    mainText = "What if this is your spiritual test?",
                    subText = "The universe might be asking: can you find fulfillment without consumption? Your answer shapes your destiny.",
                    actionText = "Pass the Test",
                    actionIcon = Icons.Default.Star,
                    motivationalFooter = "Your character is tested in quiet moments like this ⭐"
                ),
                ReflectiveMessage(
                    mainText = "The world sleeps. Your willpower awakens.",
                    subText = "This moment of choice in the darkness is where your strongest self is born. What will you choose?",
                    actionText = "Choose Your Strongest Self",
                    actionIcon = Icons.Default.Bedtime,
                    motivationalFooter = "Strength is built in the dark hours 🌙"
                )
            )

            else -> getGeneralMessages(hour, isWeekend)
        }
    }

    @Composable
    fun getNewsMessages(hour: Int, isWeekend: Boolean): List<ReflectiveMessage> {
        return when {
            // Early Morning (5-8 AM)
            hour in 5..8 -> listOf(
                ReflectiveMessage(
                    mainText = "The news can wait. Your dreams cannot.",
                    subText = "World events will unfold without your morning anxiety. But your dreams need your morning energy to come alive.",
                    actionText = "Energize Your Dreams",
                    actionIcon = Icons.Default.WbSunny,
                    motivationalFooter = "Dreams die in news feeds, live in focused mornings ☀️"
                ),
                ReflectiveMessage(
                    mainText = "What if your morning mindset shapes global outcomes?",
                    subText = "When you start with peace instead of chaos, you add positive energy to the world. Your calm creates ripples.",
                    actionText = "Create Positive Ripples",
                    actionIcon = Icons.Default.Waves,
                    motivationalFooter = "Your inner peace is your contribution to world peace 🌊"
                ),
                ReflectiveMessage(
                    mainText = "Successful people create news, they don't just consume it.",
                    subText = "While others start with headlines, you could start with lifelines - connecting to your purpose and power.",
                    actionText = "Connect to Purpose",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Purpose-driven mornings create headline-worthy lives 📈"
                ),
                ReflectiveMessage(
                    mainText = "Your morning sets the world's tone.",
                    subText = "Every positive choice you make sends love into the collective consciousness. What energy will you contribute?",
                    actionText = "Contribute Love",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "Love is the most important news of all ❤️"
                ),
                ReflectiveMessage(
                    mainText = "What if you became the good news?",
                    subText = "Instead of consuming stories of what's wrong, you could create stories of what's right. Your life is your message.",
                    actionText = "Become Good News",
                    actionIcon = Icons.Default.Star,
                    motivationalFooter = "Be the change you wish to see in the headlines ⭐"
                )
            )

            // Mid Morning (9-11 AM)
            hour in 9..11 -> listOf(
                ReflectiveMessage(
                    mainText = "Your productivity is your protest.",
                    subText = "While the world argues, you could be building. Your focused work is your vote for a better future.",
                    actionText = "Vote Through Work",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Productivity is the highest form of activism 💼"
                ),
                ReflectiveMessage(
                    mainText = "Headlines are designed to hijack your attention.",
                    subText = "Your attention is your most valuable currency. Are you spending it on fear or investing it in your future?",
                    actionText = "Invest in Your Future",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Your future needs your attention more than the news does 🚀"
                ),
                ReflectiveMessage(
                    mainText = "What if your skills could solve what you're reading about?",
                    subText = "Instead of just consuming problems, you could develop solutions. Your learning might heal what's broken.",
                    actionText = "Develop Solutions",
                    actionIcon = Icons.Default.Build,
                    motivationalFooter = "Problem solvers shape tomorrow's headlines 🔧"
                ),
                ReflectiveMessage(
                    mainText = "Every master has learned to tune out noise.",
                    subText = "Your ability to focus despite distractions is your superpower. How will you use it today?",
                    actionText = "Use Your Superpower",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Focus is the ultimate superpower 🦸"
                ),
                ReflectiveMessage(
                    mainText = "The world needs builders, not just browsers.",
                    subText = "While others scroll through civilization, you could be building it. What will you construct today?",
                    actionText = "Build Civilization",
                    actionIcon = Icons.Default.Architecture,
                    motivationalFooter = "Builders are remembered, browsers are forgotten 🏗️"
                )
            )

            // Midday (12-2 PM)
            hour in 12..14 -> listOf(
                ReflectiveMessage(
                    mainText = "Your lunch break could break the cycle.",
                    subText = "Instead of feeding your mind more chaos, what if you nourished it with peace? Your afternoon depends on this choice.",
                    actionText = "Choose Peace",
                    actionIcon = Icons.Default.Restaurant,
                    motivationalFooter = "Peaceful inputs create powerful outputs 🍽️"
                ),
                ReflectiveMessage(
                    mainText = "What if your joy is an act of rebellion?",
                    subText = "In a world obsessed with doom, your happiness is revolutionary. How will you rebel with joy today?",
                    actionText = "Rebel with Joy",
                    actionIcon = Icons.Default.Mood,
                    motivationalFooter = "Joy is the ultimate form of resistance 😊"
                ),
                ReflectiveMessage(
                    mainText = "Your energy is contagious. What will you spread?",
                    subText = "After consuming negative news, you carry that energy into every interaction. What energy do you want to share?",
                    actionText = "Spread Positive Energy",
                    actionIcon = Icons.Default.Groups,
                    motivationalFooter = "Your energy is your gift to the world 🌟"
                ),
                ReflectiveMessage(
                    mainText = "The most informed people aren't the most successful.",
                    subText = "Success comes from action, not information. What action could you take that would make today historic?",
                    actionText = "Take Historic Action",
                    actionIcon = Icons.Default.History,
                    motivationalFooter = "History is made by doers, not knowers 📚"
                ),
                ReflectiveMessage(
                    mainText = "What if you trusted the world to unfold perfectly?",
                    subText = "Your constant monitoring won't change global events, but it will change your inner peace. What matters more?",
                    actionText = "Trust and Act",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Trust in the process, focus on your progress 🌱"
                )
            )

            // Afternoon (3-5 PM)
            hour in 15..17 -> listOf(
                ReflectiveMessage(
                    mainText = "Your afternoon attention is golden.",
                    subText = "While others waste these precious hours on others' problems, you could be solving your own. What needs your attention?",
                    actionText = "Solve Your Problems",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Your problems solved become your wisdom gained 💡"
                ),
                ReflectiveMessage(
                    mainText = "What if local action beats global information?",
                    subText = "You can't fix the world's problems from your phone, but you can fix your own life. Where will you focus?",
                    actionText = "Fix Your Life",
                    actionIcon = Icons.Default.Home,
                    motivationalFooter = "Change yourself, change the world 🏠"
                ),
                ReflectiveMessage(
                    mainText = "Your story is more important than their stories.",
                    subText = "While you're reading about others' lives, yours is waiting to be lived. What chapter will you write today?",
                    actionText = "Write Your Chapter",
                    actionIcon = Icons.Default.Edit,
                    motivationalFooter = "Your story matters more than any story you'll read 📖"
                ),
                ReflectiveMessage(
                    mainText = "What if this urge is actually inspiration?",
                    subText = "Maybe your soul wants to stay informed so you can contribute solutions. What positive change could you create?",
                    actionText = "Create Positive Change",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Be the solution the world needs 🌍"
                ),
                ReflectiveMessage(
                    mainText = "The world needs your gifts, not your anxiety.",
                    subText = "Your unique talents are the world's hope. Don't let news consumption steal the time needed to develop them.",
                    actionText = "Develop Your Gifts",
                    actionIcon = Icons.Default.CardGiftcard,
                    motivationalFooter = "Your gifts are the world's greatest need 🎁"
                )
            )

            // Evening (6-8 PM)
            hour in 18..20 -> listOf(
                ReflectiveMessage(
                    mainText = "Sunsets don't need news updates.",
                    subText = "Nature continues its perfect rhythm regardless of headlines. What natural rhythm could you return to?",
                    actionText = "Return to Nature",
                    actionIcon = Icons.Default.Nature,
                    motivationalFooter = "Natural rhythms heal what news disrupts 🌅"
                ),
                ReflectiveMessage(
                    mainText = "Your evening could be sacred.",
                    subText = if (isWeekend) "Weekend evenings are for restoration. How will you restore your soul tonight?"
                    else "Work is done. This is your time to be human, not just informed. What would honor your humanity?",
                    actionText = "Honor Your Humanity",
                    actionIcon = Icons.Default.SelfImprovement,
                    motivationalFooter = "Your humanity is your greatest strength 🕊️"
                ),
                ReflectiveMessage(
                    mainText = "Love is the only news that matters.",
                    subText = "All the world's problems stem from lack of love. All solutions come from its presence. How will you love tonight?",
                    actionText = "Choose Love",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "Love is the ultimate headline ❤️"
                ),
                ReflectiveMessage(
                    mainText = "What if you created the news you want to see?",
                    subText = "Instead of consuming stories of division, you could create stories of connection. Your life is your message.",
                    actionText = "Create Unity",
                    actionIcon = Icons.Default.Groups,
                    motivationalFooter = "Unity starts with you 🤝"
                ),
                ReflectiveMessage(
                    mainText = "The most powerful people turn off the news.",
                    subText = "They know that clarity comes from silence, not noise. What clarity is waiting for you in the silence?",
                    actionText = "Find Clarity",
                    actionIcon = Icons.Default.Lightbulb,
                    motivationalFooter = "Clarity comes from within, not from without 💡"
                )
            )

            // Night (9-11 PM)
            hour in 21..23 -> listOf(
                ReflectiveMessage(
                    mainText = "Your dreams need protection from nightmares.",
                    subText = "Late-night news often becomes literal nightmares. What dreams deserve your protection tonight?",
                    actionText = "Protect Your Dreams",
                    actionIcon = Icons.Default.Bedtime,
                    motivationalFooter = "Guard your dreams like treasures 💎"
                ),
                ReflectiveMessage(
                    mainText = "The night sky doesn't read the news.",
                    subText = "Stars shine regardless of earthly drama. What light could you shine that's independent of external chaos?",
                    actionText = "Shine Your Light",
                    actionIcon = Icons.Default.Star,
                    motivationalFooter = "Your light is needed more than your anxiety ✨"
                ),
                ReflectiveMessage(
                    mainText = "What if peace is your purpose?",
                    subText = "In a world of chaos, your peace is your contribution. How will you cultivate it tonight?",
                    actionText = "Cultivate Peace",
                    actionIcon = Icons.Default.Spa,
                    motivationalFooter = "Peace is the most radical act 🧘"
                ),
                ReflectiveMessage(
                    mainText = "Your rest is an act of self-love.",
                    subText = "The world's problems will be there tomorrow, but your rest won't be. What does self-love look like tonight?",
                    actionText = "Practice Self-Love",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "Self-love is the foundation of world love ❤️"
                ),
                ReflectiveMessage(
                    mainText = "Tomorrow's solutions need tonight's rest.",
                    subText = "Your well-rested mind could solve problems that your tired, news-saturated mind cannot. What will you choose?",
                    actionText = "Choose Solutions",
                    actionIcon = Icons.Default.NightlightRound,
                    motivationalFooter = "Solutions come to rested minds 🌙"
                )
            )

            // Late Night (12-4 AM)
            hour in 0..4 -> listOf(
                ReflectiveMessage(
                    mainText = "Even journalists sleep.",
                    subText = "The people creating the news know when to stop. Your wisdom is calling you to rest.",
                    actionText = "Listen to Wisdom",
                    actionIcon = Icons.Default.NightlightRound,
                    motivationalFooter = "Wisdom whispers in the night 🦉"
                ),
                ReflectiveMessage(
                    mainText = "Your soul needs silence more than stories.",
                    subText = "In the depth of night, your soul craves peace, not more information. What does your soul need?",
                    actionText = "Honor Your Soul",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Your soul's needs are sacred 🕊️"
                ),
                ReflectiveMessage(
                    mainText = "The world's problems can't be solved at 3 AM.",
                    subText = "But your sleep problems can be solved right now. What gift will you give your future self?",
                    actionText = "Gift Yourself Rest",
                    actionIcon = Icons.Default.Bedtime,
                    motivationalFooter = "Rest is the ultimate gift to yourself 💤"
                ),
                ReflectiveMessage(
                    mainText = "What if this is your sacred surrender?",
                    subText = "Letting go of the need to know everything is spiritual maturity. What are you ready to surrender?",
                    actionText = "Surrender Gracefully",
                    actionIcon = Icons.Default.SelfImprovement,
                    motivationalFooter = "Surrender is the highest form of strength 🙏"
                ),
                ReflectiveMessage(
                    mainText = "The universe operates perfectly without your oversight.",
                    subText = "Your constant monitoring won't improve anything, but your rest will improve everything. Trust the process.",
                    actionText = "Trust and Rest",
                    actionIcon = Icons.Default.Star,
                    motivationalFooter = "Trust in the divine timing of all things ⭐"
                )
            )

            else -> getGeneralMessages(hour, isWeekend)
        }
    }

    @Composable
    fun getGeneralMessages(hour: Int, isWeekend: Boolean): List<ReflectiveMessage> {
        return when {
            // Early Morning (5-8 AM)
            hour in 5..8 -> listOf(
                ReflectiveMessage(
                    mainText = "Rise and shine, champion! Your day is a blank canvas.",
                    subText = "The most successful people know that mornings are magic. This is your golden hour to set intentions that will echo through your entire day. What legacy do you want to build today?",
                    actionText = "Ignite Your Purpose",
                    actionIcon = Icons.Default.WbSunny,
                    motivationalFooter = "Champions are made in the morning silence ☀️"
                ),
                ReflectiveMessage(
                    mainText = "The world is asleep, but your dreams are awake.",
                    subText = "While others waste their mornings scrolling, you have the power to do something extraordinary. Your future self is counting on the choices you make in the next 60 minutes.",
                    actionText = "Choose Greatness",
                    actionIcon = Icons.Default.Palette,
                    motivationalFooter = "Your morning ritual creates your evening victory 🎨"
                ),
                ReflectiveMessage(
                    mainText = "This moment decides everything.",
                    subText = "How you spend your first hour awake literally rewires your brain for success or mediocrity. You're not just starting a day—you're sculpting your character.",
                    actionText = "Forge Your Path",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Excellence is a morning habit 🧠"
                ),
                ReflectiveMessage(
                    mainText = "The early bird doesn't just catch the worm—it owns the sky.",
                    subText = "Every minute you spend here is a minute stolen from your potential. The most powerful version of yourself is waiting—but only if you choose to meet them.",
                    actionText = "Meet Your Potential",
                    actionIcon = Icons.Default.FlightTakeoff,
                    motivationalFooter = "Your breakthrough is one morning routine away 🚀"
                ),
                ReflectiveMessage(
                    mainText = "Morning energy is liquid gold—don't waste it.",
                    subText = "This isn't just another morning. This is THE morning where everything changes. Your competitors are sleeping, your excuses are silent, and your potential is unlimited.",
                    actionText = "Seize the Dawn",
                    actionIcon = Icons.Default.Diamond,
                    motivationalFooter = "Legends are born in the morning hours 💎"
                )
            )

            // Mid Morning (9-11 AM)
            hour in 9..11 -> listOf(
                ReflectiveMessage(
                    mainText = "Your brain is a Ferrari—don't drive it like a bicycle.",
                    subText = "Right now, your cognitive power is at its absolute peak. Scientists prove these are your most productive hours. Are you using this gift to scroll, or to soar?",
                    actionText = "Unleash Your Peak",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Peak performance demands peak choices 🧠"
                ),
                ReflectiveMessage(
                    mainText = "The productivity gods have blessed you with focus.",
                    subText = "This is your mental prime time—when breakthrough ideas are born and impossible tasks become possible. Every second here is worth three later. Choose wisely.",
                    actionText = "Command Your Focus",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Focus is your superpower—use it 📈"
                ),
                ReflectiveMessage(
                    mainText = "You're in the zone where dreams become reality.",
                    subText = "The gap between who you are and who you want to be can be closed in the next two hours. But only if you stop sabotaging your own success.",
                    actionText = "Bridge the Gap",
                    actionIcon = Icons.Default.Construction,
                    motivationalFooter = "Your transformation starts with this choice 🌉"
                ),
                ReflectiveMessage(
                    mainText = "This is your power hour—literally.",
                    subText = "Neuroscience shows your willpower is strongest right now. You could accomplish more in the next 60 minutes than most people do all day. Will you?",
                    actionText = "Harness Your Power",
                    actionIcon = Icons.Default.Bolt,
                    motivationalFooter = "Power unused is power wasted ⚡"
                ),
                ReflectiveMessage(
                    mainText = "The universe is testing your commitment.",
                    subText = "Every successful person has faced this exact moment—the choice between easy distraction and meaningful progress. Your response defines your destiny.",
                    actionText = "Pass the Test",
                    actionIcon = Icons.Default.EmojiEvents,
                    motivationalFooter = "Champions choose progress over pleasure 🏆"
                )
            )

            // Midday (12-2 PM)
            hour in 12..14 -> listOf(
                ReflectiveMessage(
                    mainText = "Midday reality check: Are you living or just existing?",
                    subText = "Half your day is gone forever. You can't rewind, but you can redirect. The most successful people use midday as their comeback moment. What's your comeback story?",
                    actionText = "Write Your Comeback",
                    actionIcon = Icons.Default.SelfImprovement,
                    motivationalFooter = "Every comeback starts with a conscious choice 🧘"
                ),
                ReflectiveMessage(
                    mainText = "You're at the crossroads of your day.",
                    subText = "This is where average people surrender to distraction and extraordinary people double down on their dreams. Which path will you choose?",
                    actionText = "Choose Extraordinary",
                    actionIcon = Icons.Default.Edit,
                    motivationalFooter = "Extraordinary lives are built on ordinary moments 📝"
                ),
                ReflectiveMessage(
                    mainText = "The afternoon holds your redemption.",
                    subText = "Maybe your morning wasn't perfect. So what? Champions aren't defined by perfect starts—they're defined by powerful finishes. Your second act starts now.",
                    actionText = "Start Your Second Act",
                    actionIcon = Icons.Default.Refresh,
                    motivationalFooter = "Every moment is a chance to begin again 🔄"
                ),
                ReflectiveMessage(
                    mainText = "You're searching for something—but is it here?",
                    subText = "That restless feeling brought you here, but what you're really seeking isn't in this app. It's in taking action on your dreams. Stop searching and start building.",
                    actionText = "Start Building",
                    actionIcon = Icons.Default.Build,
                    motivationalFooter = "Builders create what seekers only dream of 🔨"
                ),
                ReflectiveMessage(
                    mainText = "Midday is your momentum moment.",
                    subText = "You can either let the day happen to you, or you can happen to the day. The choice you make right now will echo through your evening and into tomorrow.",
                    actionText = "Create Momentum",
                    actionIcon = Icons.Default.Speed,
                    motivationalFooter = "Momentum is the bridge between dreams and reality 🌊"
                )
            )

            // Afternoon (3-5 PM)
            hour in 15..17 -> listOf(
                ReflectiveMessage(
                    mainText = "The afternoon slump is a myth for winners.",
                    subText = "While others crash, champions rise. This is when your discipline separates you from the pack. Your competition is getting tired—you're getting stronger.",
                    actionText = "Rise Above",
                    actionIcon = Icons.Default.DirectionsWalk,
                    motivationalFooter = "Winners walk when others crawl 🚶"
                ),
                ReflectiveMessage(
                    mainText = "Energy is a choice, not a feeling.",
                    subText = "You think you're tired, but you're actually bored. Your body craves movement, your mind craves challenge. Give them what they need, not what they want.",
                    actionText = "Choose Energy",
                    actionIcon = Icons.Default.Lightbulb,
                    motivationalFooter = "Energy flows where attention goes 💡"
                ),
                ReflectiveMessage(
                    mainText = "This is your character-building hour.",
                    subText = "Anyone can work when they feel like it. But the afternoon is when character is forged. Are you building the character that matches your ambitions?",
                    actionText = "Forge Character",
                    actionIcon = Icons.Default.FitnessCenter,
                    motivationalFooter = "Character is built when no one is watching 💪"
                ),
                ReflectiveMessage(
                    mainText = "The afternoon is where dreams go to die—or fly.",
                    subText = "This is the graveyard of good intentions. But not yours. You're different. You're the one who pushes through when pushing through is hard.",
                    actionText = "Push Through",
                    actionIcon = Icons.Default.Rocket,
                    motivationalFooter = "Dreams take flight on the wings of persistence 🚀"
                ),
                ReflectiveMessage(
                    mainText = "You're not tired—you're undertrained.",
                    subText = "Your discomfort isn't a signal to quit—it's a signal to grow. Every time you choose action over apathy, you're training for the life you want.",
                    actionText = "Train for Greatness",
                    actionIcon = Icons.Default.FitnessCenter,
                    motivationalFooter = "Greatness is a practice, not a destination 🏋️"
                )
            )

            // Evening (6-8 PM)
            hour in 18..20 -> listOf(
                ReflectiveMessage(
                    mainText = "The day is ending, but your story is just beginning.",
                    subText = "How you spend your evening determines how you wake up tomorrow. Champions don't just work hard—they recover smart. What would make tonight legendary?",
                    actionText = "Make Tonight Legendary",
                    actionIcon = Icons.Default.Favorite,
                    motivationalFooter = "Legendary evenings create legendary mornings ❤️"
                ),
                ReflectiveMessage(
                    mainText = "Evening is for connection, not disconnection.",
                    subText = if (isWeekend) "Weekend evenings are sacred—they're for the people and experiences that truly matter. Don't let them slip away into the digital void."
                    else "Work is done. This is your time to reconnect with what makes you human. Choose presence over pixels.",
                    actionText = "Choose Presence",
                    actionIcon = Icons.Default.Spa,
                    motivationalFooter = "Presence is the greatest present you can give 🌙"
                ),
                ReflectiveMessage(
                    mainText = "Your evening ritual creates your morning magic.",
                    subText = "The most successful people know that evenings aren't for winding down—they're for setting up. How you end today determines how you begin tomorrow.",
                    actionText = "Set Up Success",
                    actionIcon = Icons.Default.Schedule,
                    motivationalFooter = "Tomorrow's victories are won tonight 🕐"
                ),
                ReflectiveMessage(
                    mainText = "This is your golden hour of reflection.",
                    subText = "The evening light reminds us that every day is a gift. Instead of scrolling through other people's highlights, create your own moments worth remembering.",
                    actionText = "Create Moments",
                    actionIcon = Icons.Default.CameraAlt,
                    motivationalFooter = "Memories are made, not scrolled through 📸"
                ),
                ReflectiveMessage(
                    mainText = "Evening is when wisdom whispers.",
                    subText = "In the quiet of evening, you can hear what your soul truly needs. It's not more content—it's more connection. To yourself, to others, to your purpose.",
                    actionText = "Listen to Wisdom",
                    actionIcon = Icons.Default.Hearing,
                    motivationalFooter = "Wisdom speaks in the language of stillness 🤫"
                )
            )

            // Night (9-11 PM)
            hour in 21..23 -> listOf(
                ReflectiveMessage(
                    mainText = "Your mind is a garden—what are you planting before sleep?",
                    subText = "The last thoughts you feed your mind become the first thoughts you wake up with. Plant seeds of possibility, not weeds of worry.",
                    actionText = "Plant Possibility",
                    actionIcon = Icons.Default.Bedtime,
                    motivationalFooter = "Beautiful mornings grow from mindful nights 😴"
                ),
                ReflectiveMessage(
                    mainText = "Sleep is your secret weapon—don't sabotage it.",
                    subText = "While others sacrifice their sleep for instant gratification, you're building the foundation for tomorrow's success. Quality rest creates quality life.",
                    actionText = "Choose Quality",
                    actionIcon = Icons.Default.Star,
                    motivationalFooter = "Sleep is not a luxury—it's a necessity for excellence ⭐"
                ),
                ReflectiveMessage(
                    mainText = "The night belongs to dreamers, not scrollers.",
                    subText = "Your subconscious works while you sleep, processing and creating. Give it beautiful material to work with, not digital junk food.",
                    actionText = "Feed Your Dreams",
                    actionIcon = Icons.Default.CloudQueue,
                    motivationalFooter = "Dreams are the blueprints of tomorrow ☁️"
                ),
                ReflectiveMessage(
                    mainText = "This is your gratitude hour.",
                    subText = "Before you sleep, honor what you've accomplished today. Gratitude is the bridge between today's achievements and tomorrow's possibilities.",
                    actionText = "Honor Your Progress",
                    actionIcon = Icons.Default.Celebration,
                    motivationalFooter = "Gratitude transforms ordinary days into extraordinary memories 🎉"
                ),
                ReflectiveMessage(
                    mainText = "Your future self is watching—make them proud.",
                    subText = "Every night, you choose who you'll be tomorrow. The person you'll be in 10 years is shaped by the choices you make in the next 10 minutes.",
                    actionText = "Make Future You Proud",
                    actionIcon = Icons.Default.PersonPin,
                    motivationalFooter = "Your future self is your greatest cheerleader 🙌"
                )
            )

            // Late Night (12-4 AM)
            hour in 0..4 -> listOf(
                ReflectiveMessage(
                    mainText = "Champions are made in bed by midnight.",
                    subText = "The most successful people protect their sleep like their life depends on it—because it does. Your tomorrow is being stolen by your tonight.",
                    actionText = "Protect Your Tomorrow",
                    actionIcon = Icons.Default.NightlightRound,
                    motivationalFooter = "Your bed is your launchpad for tomorrow's success 🌙"
                ),
                ReflectiveMessage(
                    mainText = "This is your discipline moment.",
                    subText = "Anyone can make good choices when it's easy. But 2 AM is when real character is revealed. Choose like the champion you're becoming.",
                    actionText = "Choose Like a Champion",
                    actionIcon = Icons.Default.Bedtime,
                    motivationalFooter = "2 AM choices create 8 AM victories 💤"
                ),
                ReflectiveMessage(
                    mainText = "Your body is begging for rest—listen to it.",
                    subText = "Your body is your temple, your vehicle, your partner in achieving greatness. It's asking for what it needs. Will you honor that request?",
                    actionText = "Honor Your Body",
                    actionIcon = Icons.Default.Healing,
                    motivationalFooter = "Your body is your most faithful ally 🏃"
                ),
                ReflectiveMessage(
                    mainText = "The night is for restoration, not distraction.",
                    subText = "While you're here, your dreams are waiting. Your goals are waiting. Your potential is waiting. They'll all be there tomorrow—your sleep opportunity won't.",
                    actionText = "Choose Restoration",
                    actionIcon = Icons.Default.Spa,
                    motivationalFooter = "Restoration is the foundation of transformation 🛌"
                ),
                ReflectiveMessage(
                    mainText = "Morning you will thank night you.",
                    subText = "The best gift you can give yourself is the gift of rest. Tomorrow's energy, clarity, and joy depend on tonight's wisdom. Be wise.",
                    actionText = "Gift Yourself Rest",
                    actionIcon = Icons.Default.CardGiftcard,
                    motivationalFooter = "Rest is the ultimate act of self-love 💝"
                )
            )

            // Default case
            else -> listOf(
                ReflectiveMessage(
                    mainText = "Every moment is a choice—choose consciously.",
                    subText = "You're here for a reason. Maybe it's habit, maybe it's boredom, maybe it's avoidance. Whatever it is, you have the power to choose differently.",
                    actionText = "Choose Differently",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Conscious choices create extraordinary lives ✨"
                ),
                ReflectiveMessage(
                    mainText = "Your attention is your most valuable asset.",
                    subText = "Companies spend billions trying to capture what you're about to give away for free. Your attention shapes your reality. Where you focus, you go.",
                    actionText = "Direct Your Focus",
                    actionIcon = Icons.Default.CenterFocusStrong,
                    motivationalFooter = "Focus is the ultimate currency of success 🎯"
                ),
                ReflectiveMessage(
                    mainText = "This pause is your power moment.",
                    subText = "In the space between stimulus and response lies your power. You paused. You questioned. You're already winning. What will you choose next?",
                    actionText = "Use Your Power",
                    actionIcon = Icons.Default.PowerSettingsNew,
                    motivationalFooter = "Power lies in the pause ⚡"
                ),
                ReflectiveMessage(
                    mainText = "You're the CEO of your own attention.",
                    subText = "Every scroll, every tap, every minute spent here is an investment decision. Are you investing in your future or someone else's profits?",
                    actionText = "Invest Wisely",
                    actionIcon = Icons.Default.TrendingUp,
                    motivationalFooter = "Your attention is your fortune—spend it wisely 💰"
                ),
                ReflectiveMessage(
                    mainText = "The fact that you're reading this proves you're different.",
                    subText = "Most people mindlessly scroll. You stopped to think. You questioned. You paused. This consciousness is your superpower. Use it.",
                    actionText = "Use Your Superpower",
                    actionIcon = Icons.Default.Psychology,
                    motivationalFooter = "Consciousness is the ultimate competitive advantage 🧠"
                )
            )
        }
    }

    data class ReflectiveMessage(
        val mainText: String,
        val subText: String,
        val actionText: String,
        val actionIcon: ImageVector,
        val motivationalFooter: String
    )
}