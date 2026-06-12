package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberGreen,
    secondary = CyberBlue,
    tertiary = CyberPurple,
    background = CyberDarkBg,
    surface = CyberDarkBg,
    onPrimary = CyberDarkBg,
    onSecondary = CyberTextColor,
    onBackground = CyberTextColor,
    onSurface = CyberTextColor,
)

private val LightColorScheme = lightColorScheme(
    primary = LightGreen,
    secondary = LightBlue,
    tertiary = LightPurple,
    background = LightBg,
    surface = LightCard,
    onPrimary = LightBg,
    onSecondary = LightTextColor,
    onBackground = LightTextColor,
    onSurface = LightTextColor,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
