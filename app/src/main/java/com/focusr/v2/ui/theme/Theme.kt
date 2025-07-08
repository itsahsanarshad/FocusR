package com.focusr.v2.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2D9CDB),              // Medium Blue
    onPrimary = Color(0xFF001F2F),

    primaryContainer = Color(0xFF145374),     // Darker Blue
    onPrimaryContainer = Color(0xFFD1ECFF),

    secondary = Color(0xFF56CCF2),            // Bright Sky Blue
    onSecondary = Color(0xFF001E2D),

    secondaryContainer = Color(0xFF1B3B4D),
    onSecondaryContainer = Color(0xFFB3E5FC),

    tertiary = Color(0xFF27AE60),             // Deep Green
    onTertiary = Color(0xFFD1FADF),

    background = Color(0xFF023047),           // Deep Navy Blue
    onBackground = Color(0xFFD6EFFF),

    surface = Color(0xFF0A1F2D),              // Slightly lighter navy
    onSurface = Color(0xFFD6EFFF),

    error = Color(0xFFEB5757),
    onError = Color.White
)


//private val LightColorScheme = lightColorScheme(
//
//
////    primary = Color(0xFF56CCF2),              // Sky Blue
////    onPrimary = Color.White,
////
////    primaryContainer = Color(0xFFB3E5FC),     // Light Cyan-Blue
////    onPrimaryContainer = Color(0xFF00344A),
////
////    secondary = Color(0xFF2D9CDB),            // Medium Blue
////    onSecondary = Color.White,
////
////    secondaryContainer = Color(0xFFBBDEFB),
////    onSecondaryContainer = Color(0xFF002F4B),
////
////    tertiary = Color(0xFF6FCF97),             // Fresh Mint
////    onTertiary = Color(0xFF003820),
////
////    background = Color(0xFFE0F7FA),           // Very Light Cyan
////    onBackground = Color(0xFF0A1F2D),         // Deep Navy for Text
////
////    surface = Color(0xFFE3F2FD),              // Soft Light Blue
////    onSurface = Color(0xFF0A1F2D),
////
////    error = Color(0xFFEB5757),                // Soft Red
////    onError = Color.White
//)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6C63FF),              // Purple Accent
    onPrimary = Color.White,

    primaryContainer = Color(0xFF4A47A3),     // Darker Purple
    onPrimaryContainer = Color(0xFFE8E4FF),

    secondary = Color(0xFF4ECDC4),            // Teal Accent
    onSecondary = Color(0xFF003A36),

    secondaryContainer = Color(0xFF2E7D78),
    onSecondaryContainer = Color(0xFFB8F5F0),

    tertiary = Color(0xFFFF6B6B),             // Coral Accent
    onTertiary = Color(0xFF3C0000),

    background = Color(0xFF1A1A2E),           // Dark Navy Base
    onBackground = Color(0xFFE8E8F0),         // Light Text

    surface = Color(0xFF16213E),              // Dark Blue Surface
    onSurface = Color(0xFFE8E8F0),

    surfaceVariant = Color(0xFF0F3460),       // Deepest Blue
    onSurfaceVariant = Color(0xFFB8C4D6),

    outline = Color(0xFF4A5568),
    outlineVariant = Color(0xFF2D3748),

    error = Color(0xFFFF6B6B),                // Coral Error
    onError = Color.White,

    errorContainer = Color(0xFF4A1A1A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Additional glassmorphism-specific colors
    surfaceTint = Color(0xFF6C63FF),
    inverseSurface = Color(0xFFE8E8F0),
    inverseOnSurface = Color(0xFF1A1A2E),
    inversePrimary = Color(0xFF4A47A3)
)

@Composable
fun OpalForAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}