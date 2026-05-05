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
