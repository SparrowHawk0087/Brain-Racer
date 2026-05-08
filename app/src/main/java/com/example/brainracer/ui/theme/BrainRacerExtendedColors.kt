package com.example.brainracer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Цвета вне стандартного [androidx.compose.material3.ColorScheme]: таббар, сложность, градиенты, Quiz Detail.
 */
@Immutable
data class BrainRacerExtendedColors(
    val tabBarBackground: Color,
    val tabInactive: Color,
    val statusOrange: Color,
    val shadowOnDark: Color,
    val cardGradients: List<List<Color>>,
    val topicBarColors: List<Color>,
    val difficultyEasy: Color,
    val difficultyMedium: Color,
    val difficultyHard: Color,
    val difficultyExpert: Color,
    val detailBackground: Color,
    val detailSurface: Color,
    val detailSurfaceAlt: Color,
    val detailAccentPurple: Color,
    val detailBlue: Color,
    val detailGreen: Color,
    val detailOrange: Color,
    val detailTextPrimary: Color
)

fun brainRacerExtendedColorsDark(): BrainRacerExtendedColors = BrainRacerExtendedColors(
    tabBarBackground = BrainRacerColorTokens.Dark.SurfaceCard,
    tabInactive = BrainRacerColorTokens.TabInactiveDark,
    statusOrange = BrainRacerColorTokens.StatusOrange,
    shadowOnDark = Color.Black.copy(alpha = 0.35f),
    cardGradients = BrainRacerColorTokens.CardGradientsDark,
    topicBarColors = BrainRacerColorTokens.TopicBarColors,
    difficultyEasy = BrainRacerColorTokens.DifficultyEasy,
    difficultyMedium = BrainRacerColorTokens.DifficultyMedium,
    difficultyHard = BrainRacerColorTokens.DifficultyHard,
    difficultyExpert = BrainRacerColorTokens.DifficultyExpert,
    detailBackground = BrainRacerColorTokens.DetailBackground,
    detailSurface = BrainRacerColorTokens.DetailSurface,
    detailSurfaceAlt = BrainRacerColorTokens.DetailSurfaceAlt,
    detailAccentPurple = BrainRacerColorTokens.DetailAccentPurple,
    detailBlue = BrainRacerColorTokens.DetailBlue,
    detailGreen = BrainRacerColorTokens.DetailGreen,
    detailOrange = BrainRacerColorTokens.DetailOrange,
    detailTextPrimary = BrainRacerColorTokens.DetailTextPrimary
)

fun brainRacerExtendedColorsLight(): BrainRacerExtendedColors = BrainRacerExtendedColors(
    tabBarBackground = BrainRacerColorTokens.Light.SurfaceCard,
    tabInactive = BrainRacerColorTokens.Light.TextSecondary,
    statusOrange = BrainRacerColorTokens.StatusOrange,
    shadowOnDark = Color.Black.copy(alpha = 0.12f),
    cardGradients = BrainRacerColorTokens.CardGradientsLight,
    topicBarColors = BrainRacerColorTokens.TopicBarColors,
    difficultyEasy = BrainRacerColorTokens.DifficultyEasy,
    difficultyMedium = BrainRacerColorTokens.DifficultyMedium,
    difficultyHard = BrainRacerColorTokens.DifficultyHard,
    difficultyExpert = BrainRacerColorTokens.DifficultyExpert,
    detailBackground = BrainRacerColorTokens.Light.Background,
    detailSurface = BrainRacerColorTokens.Light.SurfaceCard,
    detailSurfaceAlt = BrainRacerColorTokens.Light.SurfaceElevated,
    detailAccentPurple = BrainRacerColorTokens.DetailAccentPurple,
    detailBlue = BrainRacerColorTokens.DetailBlue,
    detailGreen = BrainRacerColorTokens.DetailGreen,
    detailOrange = BrainRacerColorTokens.DetailOrange,
    detailTextPrimary = BrainRacerColorTokens.Light.TextPrimary
)

/**
 * Используется [compositionLocalOf], а не `static`, потому что значение анимируется при смене
 * темы (см. `rememberAnimatedExtendedColors` в `Theme.kt`). С `staticCompositionLocalOf`
 * любое изменение значения инвалидировало бы всё поддерево на каждом кадре анимации.
 */
val LocalBrainRacerExtendedColors = compositionLocalOf { brainRacerExtendedColorsDark() }
