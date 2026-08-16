package com.archeryscored.app.ui.review

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

enum class OverlayMode { TAP_CENTER, TAP_EDGE, PLACE_POINTS }

data class OverlayPoint(
    val id: Long,
    val xPx: Float,
    val yPx: Float,
    val score: Int,
    val isX: Boolean,
    val confirmed: Boolean
)

/**
 * Displays the captured target photo with a draggable/tappable correction layer on top:
 * two calibration taps (center, then edge) establish ring geometry, then taps add arrows,
 * drags move them, and long-presses delete them. All coordinates in callbacks are in the
 * original bitmap's pixel space, independent of how the image is scaled on screen.
 */
@Composable
fun TargetOverlay(
    bitmap: Bitmap,
    mode: OverlayMode,
    center: Offset?,
    radiusPx: Float?,
    points: List<OverlayPoint>,
    onCalibrationTap: (Offset) -> Unit,
    onAddPoint: (Offset) -> Unit,
    onMovePoint: (id: Long, newPosition: Offset) -> Unit,
    onDeletePoint: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmapWidth = bitmap.width.toFloat()
    val bitmapHeight = bitmap.height.toFloat()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val boxWidthPx = with(density) { maxWidth.toPx() }
        val scale = if (bitmapWidth > 0f) boxWidthPx / bitmapWidth else 1f
        val boxHeightDp = with(density) { (bitmapHeight * scale).toDp() }

        Box(
            Modifier
                .fillMaxWidth()
                .height(boxHeightDp)
                .pointerInput(mode) {
                    detectTapGestures(onTap = { offset ->
                        val pxOffset = Offset(offset.x / scale, offset.y / scale)
                        if (mode == OverlayMode.PLACE_POINTS) onAddPoint(pxOffset) else onCalibrationTap(pxOffset)
                    })
                }
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured target",
                modifier = Modifier.fillMaxWidth().height(boxHeightDp),
                contentScale = ContentScale.FillBounds
            )

            if (center != null && radiusPx != null) {
                Canvas(Modifier.fillMaxWidth().height(boxHeightDp)) {
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = radiusPx * scale,
                        center = Offset(center.x * scale, center.y * scale),
                        style = Stroke(width = 3f)
                    )
                }
            }

            points.forEach { point ->
                ArrowPointMarker(
                    point = point,
                    scale = scale,
                    onDrag = { newPxOffset -> onMovePoint(point.id, newPxOffset) },
                    onDelete = { onDeletePoint(point.id) }
                )
            }
        }
    }
}

@Composable
private fun ArrowPointMarker(
    point: OverlayPoint,
    scale: Float,
    onDrag: (Offset) -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    var dragPositionPx by remember(point.id) { mutableStateOf(Offset(point.xPx, point.yPx)) }

    LaunchedEffect(point.xPx, point.yPx) {
        dragPositionPx = Offset(point.xPx, point.yPx)
    }

    val markerSizeDp = 28.dp
    val markerSizePx = with(density) { markerSizeDp.toPx() }
    val offset = IntOffset(
        (dragPositionPx.x * scale - markerSizePx / 2).roundToInt(),
        (dragPositionPx.y * scale - markerSizePx / 2).roundToInt()
    )

    Box(
        modifier = Modifier
            .offset { offset }
            .size(markerSizeDp)
            .clip(CircleShape)
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
            color = if (point.confirmed) Color(0xFF4CAF50) else Color(0xFFE53935),
            fontSize = 14.sp,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
