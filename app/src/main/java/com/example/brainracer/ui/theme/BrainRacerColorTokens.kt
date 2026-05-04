package com.example.brainracer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Единый источник цветовых hex для Brain Racer.
 * Экраны не дублируют значения.
 */
object BrainRacerColorTokens {

    val Accent: Color = Color(0xFF667EEA)
    val AccentSecondary: Color = Color(0xFF764BA2)
    val OnAccent: Color = Color.White

    /** Вторичный текст на тёмном фоне (неактивные иконки). */
    val TabInactiveDark: Color = Color(0xFF8B8AAE)

    /** Статусы вызовов: ожидание, ничья. */
    val StatusOrange: Color = Color(0xFFFFA726)

    object Dark {
        val Background: Color = Color(0xFF0F0F1A)
        val SurfaceCard: Color = Color(0xFF1A1A2E)
        val SurfaceElevated: Color = Color(0xFF25243A)
        val Border: Color = Color(0xFF2A2A3E)
        val TextPrimary: Color = Color(0xFFFFFFFF)
        val TextSecondary: Color = Color(0xFF8B8AAE)
        val Error: Color = Color(0xFFEA5C7E)
        val OnError: Color = Color.White
        val ErrorContainer: Color = Color(0xFF3D1F28)
        val PrimaryContainer: Color = Color(0xFF3D4F9E)
        val OnPrimaryContainer: Color = Color(0xFFE8EAFF)
        val Scrim: Color = Color(0xCC000000)
        val InverseSurface: Color = Color(0xFFE8E8F0)
        val InverseOnSurface: Color = Color(0xFF1A1A2E)
        val InversePrimary: Color = Color(0xFF9FA8FF)
    }

    object Light {
        val Background: Color = Color(0xFFF0F2FA)
        val SurfaceCard: Color = Color(0xFFFFFFFF)
        val SurfaceElevated: Color = Color(0xFFE8EBF5)
        val Border: Color = Color(0xFFD0D4E8)
        val TextPrimary: Color = Color(0xFF1A1A2E)
        val TextSecondary: Color = Color(0xFF5A5878)
        val Error: Color = Color(0xFFD32F5A)
        val OnError: Color = Color.White
        val ErrorContainer: Color = Color(0xFFFFDAD8)
        val OnErrorContainer: Color = Color(0xFF5F1125)
        val PrimaryContainer: Color = Color(0xFFDDE0FF)
        val OnPrimaryContainer: Color = Color(0xFF1F2B6E)
        val Scrim: Color = Color(0x66000000)
        val InverseSurface: Color = Color(0xFF2A2A3E)
        val InverseOnSurface: Color = Color(0xFFF0F2FA)
        val InversePrimary: Color = Color(0xFF667EEA)
    }

    /** Градиенты карточек викторин (как на HomeScreen). */
    val CardGradientsDark: List<List<Color>> = listOf(
        listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
        listOf(Color(0xFFf093fb), Color(0xFFf5576c)),
        listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
        listOf(Color(0xFF43e97b), Color(0xFF38f9d7)),
        listOf(Color(0xFFfa709a), Color(0xFFfee140)),
        listOf(Color(0xFFa18cd1), Color(0xFFfbc2eb))
    )

    val CardGradientsLight: List<List<Color>> = CardGradientsDark

    val TopicBarColors: List<Color> = listOf(
        Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF4CAF50),
        Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFF00BCD4)
    )

    val DifficultyEasy: Color = Color(0xFF43e97b)
    val DifficultyMedium: Color = Color(0xFF4facfe)
    val DifficultyHard: Color = Color(0xFFf5576c)
    val DifficultyExpert: Color = Color(0xFFFFD700)

    /** Акцент строки награды на экране результата викторины. */
    val QuizXpBonusAccent: Color = Color(0xFF4facfe)

    /** Плашка «А ты крут!» и т.п. на результате. */
    val QuizResultEncouragement: Color = Color(0xFFFF7043)

    /** Текст ошибки поля (auth / валидация). */
    val InputValidationError: Color = Color(0xFFFF8A8A)

    /** Верхний стоп градиента обложки на экране детали викторины. */
    val DetailHeroGradientStart: Color = Color(0xFF6C63FF)

    val DetailBackground: Color = Color(0xFF12111A)
    val DetailSurface: Color = Color(0xFF1E1D2B)
    val DetailSurfaceAlt: Color = Color(0xFF25243A)
    val DetailAccentPurple: Color = Color(0xFF7C6FCD)
    val DetailBlue: Color = Color(0xFF4F9CF9)
    val DetailGreen: Color = Color(0xFF3ECFA3)
    val DetailOrange: Color = Color(0xFFF97B3E)
    val DetailTextPrimary: Color = Color(0xFFF0EFFF)
}
