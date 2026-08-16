package com.archeryscored.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Diameter of a placed arrow mark - large enough to read the score at a glance while dragging. */
val ArrowMarkerSize = 40.dp

/** How far left of the actual touch point a newly-placed mark renders, so a finger never hides it. */
private val TouchOffsetX = (-36).dp

/**
 * Converts a screen-space left-offset into the [scale]d coordinate space a caller is working in
 * (bitmap pixels for a photo, or diagram pixels for the tap-a-diagram screen), for use when placing
 * a *new* mark from a raw tap. Existing marks preserve this same offset automatically through drags,
 * since drag deltas are added on top of a position that already has it baked in.
 */
fun touchOffsetPx(scale: Float, density: Density): Float =
    with(density) { TouchOffsetX.toPx() } / scale

data class MarkerPoint(
    val id: Long,
    val xPx: Float,
    val yPx: Float,
    val score: Int,
    val isX: Boolean,
    val confirmed: Boolean
)

/**
 * A single draggable, deletable arrow mark. [scale] converts this point's px coordinates (already
 * in whatever source space the caller uses) into on-screen px for rendering and for interpreting
 * drag deltas - pass 1f if the caller's coordinate space already matches the screen.
 */
@Composable
fun ArrowMarker(
    point: MarkerPoint,
    scale: Float,
    onDrag: (Offset) -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    var dragPositionPx by remember(point.id) { mutableStateOf(Offset(point.xPx, point.yPx)) }

    LaunchedEffect(point.xPx, point.yPx) {
        dragPositionPx = Offset(point.xPx, point.yPx)
    }

    val markerSizePx = with(density) { ArrowMarkerSize.toPx() }
    val offset = IntOffset(
        (dragPositionPx.x * scale - markerSizePx / 2).roundToInt(),
        (dragPositionPx.y * scale - markerSizePx / 2).roundToInt()
    )
    val fillColor = if (point.confirmed) Color(0xFF2E7D32) else Color(0xFFC62828)

    Box(
        modifier = Modifier
            .offset { offset }
            .size(ArrowMarkerSize)
            .shadow(4.dp, CircleShape)
            .background(fillColor, CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .pointerInput(point.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragPositionPx = Offset(
                            dragPositionPx.x + dragAmount.x / scale,
                            dragPositionPx.y + dragAmount.y / scale
                        )
                        onDrag(dragPositionPx)
                    }
                )
            }
            .pointerInput(point.id) {
                detectTapGestures(onLongPress = { onDelete() })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (point.isX) "X" else point.score.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
