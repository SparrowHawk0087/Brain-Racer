package com.example.brainracer.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

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

/**
 * Длительность общего fade-out оверлея. Подобрана так, чтобы анимация ощущалась
 * заметной, но не затянутой; совпадает с продолжительностью animateColorAsState
 * для статус-/навбара, чтобы они выходили на новые цвета синхронно с экраном.
 */
private const val THEME_TRANSITION_DURATION_MS = 650

/**
 * Через какое время после начала перехода переключаем тон системных бар-иконок
 * (светлые ↔ тёмные). На середине fade-out оверлей уже наполовину прозрачен,
 * статус-бар уже близок к новому цвету — флип тона на этом моменте не виден,
 * а на t=0 он бы дал «битые» (невидимые) иконки на ещё-старом фоне.
 */
private const val THEME_SYSTEM_BAR_TONE_DELAY_MS = 220L

/**
 * Глобальная тема Brain Racer.
 *
 * Стратегия плавного перехода — **crossfade-оверлей**, а не пер-цветная анимация:
 *
 * 1. [MaterialTheme] и [LocalBrainRacerExtendedColors] получают палитру **мгновенно**
 *    — без `target.copy(...)` каждый кадр. Это убирает 50+ одновременных
 *    `animateColorAsState`, которые каждый кадр пересоздавали `ColorScheme`
 *    и инвалидировали всех читателей темы (каждый Text, Surface, Card в дереве).
 *
 * 2. Поверх контента рисуется [ThemeCrossfadeOverlay]: на момент переключения
 *    snap'ом ставится `alpha = 1f` (полное закрытие старым фоном), под оверлеем
 *    мгновенно применяется новая палитра, и оверлей плавно гасится за
 *    [THEME_TRANSITION_DURATION_MS] одной [Animatable]-анимацией.
 *    Перерисовка ограничена `graphicsLayer { alpha = ... }` одного `Box` —
 *    не задевая ни один читатель `MaterialTheme.colorScheme.*`.
 *
 * 3. Системные статус/навбар анимируются через [SystemBarsThemeAnimator]:
 *    один animateColorAsState на цвет, дешёвая запись `window.statusBarColor`
 *    в SideEffect. Тон бар-иконок (`isAppearanceLight*`) флипается единожды
 *    через `LaunchedEffect` с задержкой [THEME_SYSTEM_BAR_TONE_DELAY_MS].
 */
@Composable
fun BrainRacerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(darkTheme) {
        if (darkTheme) brainRacerDarkColorScheme() else brainRacerLightColorScheme()
    }
    val extended = remember(darkTheme) {
        if (darkTheme) brainRacerExtendedColorsDark() else brainRacerExtendedColorsLight()
    }

    SystemBarsThemeAnimator(darkTheme = darkTheme)

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBrainRacerExtendedColors provides extended) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                shapes = Shapes,
                content = content
            )
        }
        ThemeCrossfadeOverlay(darkTheme = darkTheme)
    }
}

/**
 * Анимация цвета и тона системных бар. Изолирована в отдельный composable:
 * recomposition внутри animateColorAsState затрагивает только эту функцию,
 * а не родителя [BrainRacerTheme] — поэтому всё дерево контента не
 * пересобирается каждый кадр анимации.
 */
@Composable
private fun SystemBarsThemeAnimator(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val targetBar = if (darkTheme)
        BrainRacerColorTokens.Dark.SurfaceCard
    else
        BrainRacerColorTokens.Light.SurfaceCard

    val barColor by animateColorAsState(
        targetValue = targetBar,
        animationSpec = tween(THEME_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing),
        label = "themeSystemBarColor"
    )
    val barColorArgb = barColor.toArgb()

    // SideEffect внутри маленького composable дешёвый: только два int-write,
    // никаких аллокаций и никаких WindowCompat.getInsetsController вызовов на каждый кадр.
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = barColorArgb
        window.navigationBarColor = barColorArgb
    }

    // Тон бар-иконок переключается ровно один раз на смене темы, с задержкой
    // в середину анимации — иначе при переходе light-dark тёмные иконки на ещё-светлом
    // (или наоборот) фоне моргают как «невидимые».
    LaunchedEffect(darkTheme, view) {
        delay(THEME_SYSTEM_BAR_TONE_DELAY_MS)
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

/**
 * Полноэкранный crossfade-оверлей.
 *
 * Логика: храним последний применённый `darkTheme`. Когда параметр меняется,
 * запоминаем фон СТАРОЙ темы как [coverColor], мгновенно ставим alpha=1
 * (`snapTo`), переключаем `lastDark` (контент под оверлеем сразу применяет
 * новую тему — пользователь этого не видит, оверлей непрозрачный) и плавно
 * гасим alpha до нуля.
 *
 * Цена анимации — единственный `Animatable<Float>` и `graphicsLayer` оверлея.
 * Никакого пересоздания ColorScheme, никакой инвалидации `MaterialTheme.colorScheme.*`.
 *
 * `pointerInput` блокирует тапы на время перехода — иначе клик «сквозь» оверлей
 * мог бы прилететь в уже новый, но визуально ещё закрытый UI.
 */
@Composable
private fun ThemeCrossfadeOverlay(darkTheme: Boolean) {
    var lastDark by remember { mutableStateOf(darkTheme) }
    var coverColor by remember { mutableStateOf<Color?>(null) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(darkTheme) {
        if (darkTheme != lastDark) {
            coverColor = if (lastDark)
                BrainRacerColorTokens.Dark.Background
            else
                BrainRacerColorTokens.Light.Background
            alpha.snapTo(1f)
            lastDark = darkTheme
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = THEME_TRANSITION_DURATION_MS,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    val color = coverColor
    if (alpha.value > 0f && color != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha.value }
                .background(color)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                }
        )
    }
}
