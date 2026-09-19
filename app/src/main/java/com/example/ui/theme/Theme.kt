package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private fun buildDarkColorScheme(colorKey: String): ColorScheme {
    return when (colorKey.uppercase()) {
        "SAPPHIRE" -> darkColorScheme(
            primary = SapphireLight,
            onPrimary = Color(0xFF002266),
            primaryContainer = Color(0xFF1E3A8A),
            onPrimaryContainer = SapphireLight,
            secondary = CyanAccent,
            onSecondary = Color.Black,
            tertiary = AmberWarning,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color(0xFF9CA3AF),
            outline = DarkOutline,
            error = RoseError
        )
        "AMETHYST" -> darkColorScheme(
            primary = AmethystLight,
            onPrimary = Color(0xFF2E0854),
            primaryContainer = Color(0xFF4C1D95),
            onPrimaryContainer = AmethystLight,
            secondary = RoseLight,
            onSecondary = Color.Black,
            tertiary = CyanAccent,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color(0xFF9CA3AF),
            outline = DarkOutline,
            error = RoseError
        )
        "AMBER" -> darkColorScheme(
            primary = AmberLight,
            onPrimary = Color(0xFF451A03),
            primaryContainer = Color(0xFF78350F),
            onPrimaryContainer = AmberLight,
            secondary = EmeraldLight,
            onSecondary = Color.Black,
            tertiary = SapphireLight,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color(0xFF9CA3AF),
            outline = DarkOutline,
            error = RoseError
        )
        "ROSE" -> darkColorScheme(
            primary = RoseLight,
            onPrimary = Color(0xFF4C0519),
            primaryContainer = Color(0xFF881337),
            onPrimaryContainer = RoseLight,
            secondary = AmethystLight,
            onSecondary = Color.Black,
            tertiary = AmberLight,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color(0xFF9CA3AF),
            outline = DarkOutline,
            error = RoseError
        )
        "CYAN" -> darkColorScheme(
            primary = CyanLight,
            onPrimary = Color(0xFF083344),
            primaryContainer = Color(0xFF164E63),
            onPrimaryContainer = CyanLight,
            secondary = SapphireLight,
            onSecondary = Color.Black,
            tertiary = EmeraldLight,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color(0xFF9CA3AF),
            outline = DarkOutline,
            error = RoseError
        )
        else -> darkColorScheme( // Default EMERALD
            primary = EmeraldPrimary,
            onPrimary = Color(0xFF003822),
            primaryContainer = Color(0xFF005234),
            onPrimaryContainer = EmeraldLight,
            secondary = SapphireSecondary,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF1E3A8A),
            onSecondaryContainer = SapphireLight,
            tertiary = CyanAccent,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color(0xFF9CA3AF),
            outline = DarkOutline,
            error = RoseError
        )
    }
}

private fun buildLightColorScheme(colorKey: String): ColorScheme {
    return when (colorKey.uppercase()) {
        "SAPPHIRE" -> lightColorScheme(
            primary = SapphireDark,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDBEAFE),
            onPrimaryContainer = Color(0xFF1E40AF),
            secondary = CyanDark,
            onSecondary = Color.White,
            tertiary = AmberDark,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF64748B),
            outline = LightOutline,
            error = RoseError
        )
        "AMETHYST" -> lightColorScheme(
            primary = AmethystDark,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEDE9FE),
            onPrimaryContainer = Color(0xFF5B21B6),
            secondary = RoseDark,
            onSecondary = Color.White,
            tertiary = CyanDark,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF64748B),
            outline = LightOutline,
            error = RoseError
        )
        "AMBER" -> lightColorScheme(
            primary = AmberDark,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFEF3C7),
            onPrimaryContainer = Color(0xFF92400E),
            secondary = EmeraldDark,
            onSecondary = Color.White,
            tertiary = SapphireDark,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF64748B),
            outline = LightOutline,
            error = RoseError
        )
        "ROSE" -> lightColorScheme(
            primary = RoseDark,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE4E6),
            onPrimaryContainer = Color(0xFF9F1239),
            secondary = AmethystDark,
            onSecondary = Color.White,
            tertiary = AmberDark,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF64748B),
            outline = LightOutline,
            error = RoseError
        )
        "CYAN" -> lightColorScheme(
            primary = CyanDark,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCFFAFE),
            onPrimaryContainer = Color(0xFF155E75),
            secondary = SapphireDark,
            onSecondary = Color.White,
            tertiary = EmeraldDark,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF64748B),
            outline = LightOutline,
            error = RoseError
        )
        else -> lightColorScheme( // Default EMERALD
            primary = EmeraldDark,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD1FAE5),
            onPrimaryContainer = Color(0xFF065F46),
            secondary = SapphireSecondary,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFDBEAFE),
            onSecondaryContainer = Color(0xFF1E40AF),
            tertiary = CyanAccent,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF64748B),
            outline = LightOutline,
            error = RoseError
        )
    }
}

@Composable
fun EnExpenseTheme(
    themeMode: String = "SYSTEM",
    themeColor: String = "EMERALD",
    themeFont: String = "DEFAULT",
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> darkTheme
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> buildDarkColorScheme(themeColor)
        else -> buildLightColorScheme(themeColor)
    }

    val typography = getAppTypography(themeFont)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

