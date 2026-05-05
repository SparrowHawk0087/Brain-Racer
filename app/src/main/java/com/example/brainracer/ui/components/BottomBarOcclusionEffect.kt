package com.example.brainracer.ui.components

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

private fun computeBottomBarOverlapRatio(
    topYWindowPx: Float,
    heightPx: Int,
    screenHeightPx: Float,
    occlusionHeightPx: Float
): Float {
    val height = heightPx.toFloat().coerceAtLeast(1f)
    val bottom = topYWindowPx + height
    val occlusionTop = screenHeightPx - occlusionHeightPx
    val overlap = (bottom - occlusionTop).coerceAtLeast(0f)
    return (overlap / height).coerceIn(0f, 1f)
}

/**
 * Доля элемента в зоне над BottomBar → сила эффекта [0..1].
 * Пока меньше [startFraction] высоты не «ушло» под панель — эффекта нет.
 */
private fun effectStrengthFromOverlap(
    overlapRatio: Float,
    startFraction: Float
): Float {
    if (startFraction >= 1f) return 0f
    if (overlapRatio <= startFraction) return 0f
    return ((overlapRatio - startFraction) / (1f - startFraction)).coerceIn(0f, 1f)
}

/**
 * Применяет размытие/приглушение к элементу, когда он уходит под BottomBar.
 *
 * @param effectStartOverlapFraction начинать затемнение/размытие только когда не меньше этой
 *   доли высоты элемента оказалось в зоне над нижней панелью (по умолчанию половина).
 * @param maxBlur при `0.dp` — только затемнение и лёгкий сдвиг, без [Modifier.blur].
 */
fun Modifier.bottomBarOcclusionEffect(
    occlusionHeight: Dp = 92.dp,
    maxBlur: Dp = 5.dp,
    minAlpha: Float = 0.74f,
    effectStartOverlapFraction: Float = 0.5f
): Modifier = composed {
    val density = LocalDensity.current
    val view = LocalView.current
    val screenHeightPx = view.height.toFloat()
    val occlusionHeightPx = with(density) { occlusionHeight.toPx() }
    val maxBlurDp = maxBlur
    var overlapRatio by remember { mutableFloatStateOf(0f) }

    val effectStrength = effectStrengthFromOverlap(overlapRatio, effectStartOverlapFraction)

    val computedAlpha = if (effectStrength > 0f) {
        1f - ((1f - minAlpha) * effectStrength)
    } else {
        1f
    }
    val computedTranslationY = 1.5f * effectStrength

    val blurRadius = if (effectStrength > 0f && maxBlurDp > 0.dp) {
        // Мягче нарастание размытия (корень), плюс сниженный maxBlur по умолчанию
        maxBlurDp * sqrt(effectStrength.toDouble()).toFloat()
    } else {
        0.dp
    }

    val base = this
        .onGloballyPositioned { coords ->
            overlapRatio = computeBottomBarOverlapRatio(
                topYWindowPx = coords.positionInWindow().y,
                heightPx = coords.size.height,
                screenHeightPx = screenHeightPx,
                occlusionHeightPx = occlusionHeightPx
            )
        }
        .graphicsLayer(
            alpha = computedAlpha,
            translationY = computedTranslationY
        )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        blurRadius > 0.dp &&
        effectStrength > 0f
    ) {
        base.blur(blurRadius)
    } else {
        base
    }
}