package com.example.brainracer.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun bottomBarSafePadding(
    paddingValues: PaddingValues,
    extraBottom: Dp = 10.dp,
    minBottom: Dp = 0.dp
): Dp {
    return (paddingValues.calculateBottomPadding() + extraBottom).coerceAtLeast(minBottom)
}
