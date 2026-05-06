package com.example.brainracer.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun rememberFabVisibilityOnScroll(
    listState: LazyListState,
    downThresholdPx: Int = 6,
    upThresholdPx: Int = 4
): Boolean {
    var fabVisible by remember { mutableStateOf(true) }

    LaunchedEffect(listState, downThresholdPx, upThresholdPx) {
        var previousAbsoluteOffset = 0
        snapshotFlow {
            (listState.firstVisibleItemIndex * 10_000) + listState.firstVisibleItemScrollOffset
        }
            .map { absolute ->
                val delta = absolute - previousAbsoluteOffset
                previousAbsoluteOffset = absolute
                delta
            }
            .distinctUntilChanged()
            .collect { delta ->
                when {
                    delta > downThresholdPx -> fabVisible = false
                    delta < -upThresholdPx -> fabVisible = true
                    listState.firstVisibleItemIndex == 0 &&
                        listState.firstVisibleItemScrollOffset < 12 -> fabVisible = true
                }
            }
    }

    return fabVisible
}
