package com.example.brainracer.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
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

private const val THEME_TRANSITION_DURATION_MS = 550

/**
 * Анимирует переход между двумя [ColorScheme]: каждый цвет M3 интерполируется через
 * [animateColorAsState], благодаря чему смена темы выглядит плавной — фон, поверхности,
 * текст, primary/secondary плавно «перетекают» из одной палитры в другую.
 */
@Composable
private fun rememberAnimatedColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(durationMillis = THEME_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
    @Composable fun anim(c: Color, label: String): Color =
        animateColorAsState(targetValue = c, animationSpec = spec, label = label).value

    return target.copy(
        primary = anim(target.primary, "primary"),
        onPrimary = anim(target.onPrimary, "onPrimary"),
        primaryContainer = anim(target.primaryContainer, "primaryContainer"),
        onPrimaryContainer = anim(target.onPrimaryContainer, "onPrimaryContainer"),
        secondary = anim(target.secondary, "secondary"),
        onSecondary = anim(target.onSecondary, "onSecondary"),
        secondaryContainer = anim(target.secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = anim(target.onSecondaryContainer, "onSecondaryContainer"),
        tertiary = anim(target.tertiary, "tertiary"),
        onTertiary = anim(target.onTertiary, "onTertiary"),
        tertiaryContainer = anim(target.tertiaryContainer, "tertiaryContainer"),
        onTertiaryContainer = anim(target.onTertiaryContainer, "onTertiaryContainer"),
        background = anim(target.background, "background"),
        onBackground = anim(target.onBackground, "onBackground"),
        surface = anim(target.surface, "surface"),
        onSurface = anim(target.onSurface, "onSurface"),
        surfaceVariant = anim(target.surfaceVariant, "surfaceVariant"),
        onSurfaceVariant = anim(target.onSurfaceVariant, "onSurfaceVariant"),
        surfaceTint = anim(target.surfaceTint, "surfaceTint"),
        inverseSurface = anim(target.inverseSurface, "inverseSurface"),
        inverseOnSurface = anim(target.inverseOnSurface, "inverseOnSurface"),
        inversePrimary = anim(target.inversePrimary, "inversePrimary"),
        error = anim(target.error, "error"),
        onError = anim(target.onError, "onError"),
        errorContainer = anim(target.errorContainer, "errorContainer"),
        onErrorContainer = anim(target.onErrorContainer, "onErrorContainer"),
        outline = anim(target.outline, "outline"),
        outlineVariant = anim(target.outlineVariant, "outlineVariant"),
        scrim = anim(target.scrim, "scrim"),
        surfaceBright = anim(target.surfaceBright, "surfaceBright"),
        surfaceDim = anim(target.surfaceDim, "surfaceDim"),
        surfaceContainerLowest = anim(target.surfaceContainerLowest, "surfaceContainerLowest"),
        surfaceContainerLow = anim(target.surfaceContainerLow, "surfaceContainerLow"),
        surfaceContainer = anim(target.surfaceContainer, "surfaceContainer"),
        surfaceContainerHigh = anim(target.surfaceContainerHigh, "surfaceContainerHigh"),
        surfaceContainerHighest = anim(target.surfaceContainerHighest, "surfaceContainerHighest"),
    )
}

/**
 * Анимирует «расширенные» цвета приложения (вне ColorScheme): tabBar, заголовок detail-карточек,
 * границы, surface-тона. Без этого всё, что напрямую читает [LocalBrainRacerExtendedColors],
 * при переключении темы «прыгало» — теперь оно тоже плавно перетекает.
 *
 * Поля, которые одинаковы для светлой и тёмной палитры (градиенты карточек, цвета сложности,
 * topicBarColors), остаются ссылочно-стабильными — анимировать их не нужно.
 */
@Composable
private fun rememberAnimatedExtendedColors(target: BrainRacerExtendedColors): BrainRacerExtendedColors {
    val spec = tween<Color>(durationMillis = THEME_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
    @Composable fun anim(c: Color, label: String): Color =
        animateColorAsState(targetValue = c, animationSpec = spec, label = label).value

    val tabBarBackground   = anim(target.tabBarBackground,   "tabBarBackground")
    val tabInactive        = anim(target.tabInactive,        "tabInactive")
    val statusOrange       = anim(target.statusOrange,       "statusOrange")
    val shadowOnDark       = anim(target.shadowOnDark,       "shadowOnDark")
    val difficultyEasy     = anim(target.difficultyEasy,     "difficultyEasy")
    val difficultyMedium   = anim(target.difficultyMedium,   "difficultyMedium")
    val difficultyHard     = anim(target.difficultyHard,     "difficultyHard")
    val difficultyExpert   = anim(target.difficultyExpert,   "difficultyExpert")
    val detailBackground   = anim(target.detailBackground,   "detailBackground")
    val detailSurface      = anim(target.detailSurface,      "detailSurface")
    val detailSurfaceAlt   = anim(target.detailSurfaceAlt,   "detailSurfaceAlt")
    val detailAccentPurple = anim(target.detailAccentPurple, "detailAccentPurple")
    val detailBlue         = anim(target.detailBlue,         "detailBlue")
    val detailGreen        = anim(target.detailGreen,        "detailGreen")
    val detailOrange       = anim(target.detailOrange,       "detailOrange")
    val detailTextPrimary  = anim(target.detailTextPrimary,  "detailTextPrimary")

    return remember(
        tabBarBackground, tabInactive, statusOrange, shadowOnDark,
        difficultyEasy, difficultyMedium, difficultyHard, difficultyExpert,
        detailBackground, detailSurface, detailSurfaceAlt,
        detailAccentPurple, detailBlue, detailGreen, detailOrange, detailTextPrimary,
        target.cardGradients, target.topicBarColors
    ) {
        target.copy(
            tabBarBackground   = tabBarBackground,
            tabInactive        = tabInactive,
            statusOrange       = statusOrange,
            shadowOnDark       = shadowOnDark,
            difficultyEasy     = difficultyEasy,
            difficultyMedium   = difficultyMedium,
            difficultyHard     = difficultyHard,
            difficultyExpert   = difficultyExpert,
            detailBackground   = detailBackground,
            detailSurface      = detailSurface,
            detailSurfaceAlt   = detailSurfaceAlt,
            detailAccentPurple = detailAccentPurple,
            detailBlue         = detailBlue,
            detailGreen        = detailGreen,
            detailOrange       = detailOrange,
            detailTextPrimary  = detailTextPrimary
        )
    }
}

@Composable
fun BrainRacerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val targetColorScheme = if (darkTheme) brainRacerDarkColorScheme() else brainRacerLightColorScheme()
    val colorScheme = rememberAnimatedColorScheme(targetColorScheme)
    val targetExtended = if (darkTheme) brainRacerExtendedColorsDark() else brainRacerExtendedColorsLight()
    val extended = rememberAnimatedExtendedColors(targetExtended)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // colorScheme.surface уже анимирован, поэтому SideEffect будет вызываться
            // на каждом кадре анимации — статус-бар и нав-бар плавно перетекают вместе с UI.
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
