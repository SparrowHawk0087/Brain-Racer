package com.example.brainracer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun brainRacerDarkColorScheme() = darkColorScheme(
    primary = BrainRacerColorTokens.Accent,
    onPrimary = BrainRacerColorTokens.OnAccent,
    primaryContainer = BrainRacerColorTokens.Dark.PrimaryContainer,
    onPrimaryContainer = BrainRacerColorTokens.Dark.OnPrimaryContainer,
    secondary = BrainRacerColorTokens.AccentSecondary,
    onSecondary = BrainRacerColorTokens.OnAccent,
    secondaryContainer = BrainRacerColorTokens.Dark.SurfaceElevated,
    onSecondaryContainer = BrainRacerColorTokens.Dark.TextPrimary,
    tertiary = BrainRacerColorTokens.DifficultyMedium,
    onTertiary = BrainRacerColorTokens.Dark.TextPrimary,
    tertiaryContainer = BrainRacerColorTokens.Dark.SurfaceElevated,
    onTertiaryContainer = BrainRacerColorTokens.Dark.TextSecondary,
    error = BrainRacerColorTokens.Dark.Error,
    onError = BrainRacerColorTokens.Dark.OnError,
    errorContainer = BrainRacerColorTokens.Dark.ErrorContainer,
    onErrorContainer = BrainRacerColorTokens.Dark.OnError,
    background = BrainRacerColorTokens.Dark.Background,
    onBackground = BrainRacerColorTokens.Dark.TextPrimary,
    surface = BrainRacerColorTokens.Dark.SurfaceCard,
    onSurface = BrainRacerColorTokens.Dark.TextPrimary,
    surfaceVariant = BrainRacerColorTokens.Dark.Border,
    onSurfaceVariant = BrainRacerColorTokens.Dark.TextSecondary,
    outline = BrainRacerColorTokens.Dark.Border,
    outlineVariant = BrainRacerColorTokens.Dark.Border.copy(alpha = 0.5f),
    scrim = BrainRacerColorTokens.Dark.Scrim,
    inverseSurface = BrainRacerColorTokens.Dark.InverseSurface,
    inverseOnSurface = BrainRacerColorTokens.Dark.InverseOnSurface,
    inversePrimary = BrainRacerColorTokens.Dark.InversePrimary,
    surfaceDim = BrainRacerColorTokens.Dark.Background,
    surfaceBright = BrainRacerColorTokens.Dark.SurfaceElevated,
    surfaceContainerLowest = BrainRacerColorTokens.Dark.Background,
    surfaceContainerLow = BrainRacerColorTokens.Dark.SurfaceCard,
    surfaceContainer = BrainRacerColorTokens.Dark.SurfaceCard,
    surfaceContainerHigh = BrainRacerColorTokens.Dark.SurfaceElevated,
    surfaceContainerHighest = BrainRacerColorTokens.Dark.Border
)

fun brainRacerLightColorScheme() = lightColorScheme(
    primary = BrainRacerColorTokens.Accent,
    onPrimary = BrainRacerColorTokens.OnAccent,
    primaryContainer = BrainRacerColorTokens.Light.PrimaryContainer,
    onPrimaryContainer = BrainRacerColorTokens.Light.OnPrimaryContainer,
    secondary = BrainRacerColorTokens.AccentSecondary,
    onSecondary = BrainRacerColorTokens.OnAccent,
    secondaryContainer = BrainRacerColorTokens.Light.SurfaceElevated,
    onSecondaryContainer = BrainRacerColorTokens.Light.TextPrimary,
    tertiary = BrainRacerColorTokens.DifficultyMedium,
    onTertiary = BrainRacerColorTokens.Light.TextPrimary,
    tertiaryContainer = BrainRacerColorTokens.Light.SurfaceElevated,
    onTertiaryContainer = BrainRacerColorTokens.Light.TextSecondary,
    error = BrainRacerColorTokens.Light.Error,
    onError = BrainRacerColorTokens.Light.OnError,
    errorContainer = BrainRacerColorTokens.Light.ErrorContainer,
    onErrorContainer = BrainRacerColorTokens.Light.OnErrorContainer,
    background = BrainRacerColorTokens.Light.Background,
    onBackground = BrainRacerColorTokens.Light.TextPrimary,
    surface = BrainRacerColorTokens.Light.SurfaceCard,
    onSurface = BrainRacerColorTokens.Light.TextPrimary,
    surfaceVariant = BrainRacerColorTokens.Light.SurfaceElevated,
    onSurfaceVariant = BrainRacerColorTokens.Light.TextSecondary,
    outline = BrainRacerColorTokens.Light.Border,
    outlineVariant = BrainRacerColorTokens.Light.Border.copy(alpha = 0.6f),
    scrim = BrainRacerColorTokens.Light.Scrim,
    inverseSurface = BrainRacerColorTokens.Light.InverseSurface,
    inverseOnSurface = BrainRacerColorTokens.Light.InverseOnSurface,
    inversePrimary = BrainRacerColorTokens.Light.InversePrimary,
    surfaceDim = BrainRacerColorTokens.Light.SurfaceElevated,
    surfaceBright = BrainRacerColorTokens.Light.SurfaceCard,
    surfaceContainerLowest = BrainRacerColorTokens.Light.Background,
    surfaceContainerLow = BrainRacerColorTokens.Light.SurfaceElevated,
    surfaceContainer = BrainRacerColorTokens.Light.SurfaceCard,
    surfaceContainerHigh = BrainRacerColorTokens.Light.SurfaceElevated,
    surfaceContainerHighest = BrainRacerColorTokens.Light.Border
)

@Composable
fun BrainRacerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) brainRacerDarkColorScheme() else brainRacerLightColorScheme()
    val extended = if (darkTheme) brainRacerExtendedColorsDark() else brainRacerExtendedColorsLight()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                val lightBars = colorScheme.surface.luminance() > 0.5f
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }
    }

    CompositionLocalProvider(LocalBrainRacerExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
