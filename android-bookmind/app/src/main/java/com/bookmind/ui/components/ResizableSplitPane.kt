package com.bookmind.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Two-pane layout with a draggable divider. The book occupies [readerWeight] of
 * the width; the AI panel slides in on the right. Dragging the handle resizes
 * (clamped to [minReaderWeight]..[maxReaderWeight]); double-tapping the handle
 * resets to 0.65. The caller persists [readerWeight] (DataStore).
 */
@Composable
fun ResizableSplitPane(
    readerWeight: Float,
    onWeightChange: (Float) -> Unit,
    isAiVisible: Boolean,
    readerContent: @Composable () -> Unit,
    aiContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    minReaderWeight: Float = 0.3f,
    maxReaderWeight: Float = 0.85f
) {
    val haptics = LocalHapticFeedback.current
    val weightState = rememberUpdatedState(readerWeight)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        // Accumulate drag here so we don't fight the persisted source of truth.
        var dragWeight by remember(isAiVisible) { mutableFloatStateOf(readerWeight) }

        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(if (isAiVisible) dragWeight else 1f)) {
                readerContent()
            }

            AnimatedVisibility(
                visible = isAiVisible,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it }
            ) {
                Row(modifier = Modifier.fillMaxHeight()) {
                    DragDivider(
                        onDrag = { deltaPx ->
                            val next = (dragWeight + deltaPx / totalWidthPx)
                                .coerceIn(minReaderWeight, maxReaderWeight)
                            if (next != dragWeight &&
                                (next == minReaderWeight || next == maxReaderWeight)
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            dragWeight = next
                            onWeightChange(next)
                        },
                        onReset = {
                            dragWeight = 0.65f
                            onWeightChange(0.65f)
                        }
                    )
                    Box(modifier = Modifier.weight(1f - dragWeight).fillMaxHeight()) {
                        aiContent()
                    }
                }
            }
        }
    }
    // weightState read keeps the lambda honest about the latest external value.
    weightState.value
}

@Composable
private fun DragDivider(onDrag: (Float) -> Unit, onReset: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onReset() })
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.DragHandle,
            contentDescription = "Resize panels",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer { rotationZ = 90f }
        )
    }
}
