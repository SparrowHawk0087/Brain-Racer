package com.example.brainracer.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.composed
import com.example.brainracer.ui.utils.AppMotionConfig

fun Modifier.pressScale(
    enabled: Boolean = true,
    pressedScale: Float = AppMotionConfig.press.pressedScale,
    dampingRatio: Float = AppMotionConfig.press.dampingRatio,
    stiffness: Float = AppMotionConfig.press.stiffness,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
        label = "pressScale"
    )
    scale(scale)
}

fun Modifier.pressClickable(
    enabled: Boolean = true,
    pressedScale: Float = AppMotionConfig.press.pressedScale,
    indication: Indication? = null,
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    onClick: () -> Unit
): Modifier = composed {
    this
        .pressScale(
            enabled = enabled,
            pressedScale = pressedScale,
            interactionSource = interactionSource
        )
        .indication(interactionSource, indication)
        .clickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            onClick = onClick
        )
}
