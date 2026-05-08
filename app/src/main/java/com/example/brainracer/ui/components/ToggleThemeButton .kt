package com.example.brainracer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.data.preferences.UserPreferencesRepository
import com.example.brainracer.ui.viewmodels.ThemeViewModel

/**
 * Кнопка-«переключатель темы». Сама читает текущее значение из [UserPreferencesRepository]
 * и переключает его при клике.
 *
 * Анимации:
 *  - `AnimatedContent` со scale + fade между иконкой солнца и луны (плавное появление новой
 *    иконки «изнутри» с одновременным «уходом» старой — без визуального «прыжка»).
 *  - Лёгкий поворот при смене (rotate) для эффекта «переворота».
 *  - Анимированный цвет круглой подложки через [animateColorAsState] с [FastOutSlowInEasing],
 *    в такт глобальной анимации палитры темы (см. `BrainRacerTheme`).
 */
@Composable
fun ToggleThemeButton(
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefsRepo = remember(context.applicationContext) {
        UserPreferencesRepository(context.applicationContext)
    }
    val darkTheme by prefsRepo.darkTheme.collectAsStateWithLifecycle(initialValue = false)
    val cs = MaterialTheme.colorScheme

    // Длительность совпадает с глобальным переходом темы — кнопка и палитра меняются «в такт».
    val animSpec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 550, easing = FastOutSlowInEasing)
    val containerColor by animateColorAsState(
        targetValue = if (darkTheme) cs.primary.copy(alpha = 0.18f) else cs.surfaceContainerHigh,
        animationSpec = animSpec,
        label = "toggleThemeContainer"
    )
    val iconTint by animateColorAsState(
        targetValue = if (darkTheme) cs.primary else cs.onSurface.copy(alpha = 0.85f),
        animationSpec = animSpec,
        label = "toggleThemeIconTint"
    )
    val rotation by animateFloatAsState(
        targetValue = if (darkTheme) 360f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "toggleThemeRotation"
    )

    IconButton(
        onClick = { themeViewModel.toggleDarkTheme(prefsRepo, darkTheme) },
        modifier = modifier.size(36.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(containerColor)
                .pressScale(),
            contentAlignment = Alignment.Center
        ) {
            // Плавная замена иконки: новая «вырастает» из центра с fade-in,
            // старая одновременно уменьшается и угасает.
            AnimatedContent(
                targetState = darkTheme,
                transitionSpec = {
                    (scaleIn(tween(260, easing = FastOutSlowInEasing), initialScale = 0.7f) +
                            fadeIn(tween(220, easing = FastOutSlowInEasing))) togetherWith
                            (scaleOut(tween(220, easing = FastOutSlowInEasing), targetScale = 1.15f) +
                                    fadeOut(tween(180, easing = FastOutSlowInEasing)))
                },
                label = "toggleThemeIcon"
            ) { isDark ->
                Icon(
                    imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = if (isDark) "Светлая тема" else "Тёмная тема",
                    tint = iconTint,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}
