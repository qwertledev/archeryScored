package com.archeryscored.app.ui.review

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.archeryscored.app.ui.common.ArrowMarker
import com.archeryscored.app.ui.common.MarkerPoint
import com.archeryscored.app.ui.common.touchOffsetPx
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class OverlayPoint(
    val id: Long,
    val xPx: Float,
    val yPx: Float,
    val score: Int,
    val isX: Boolean,
    val confirmed: Boolean
)

private val HandleSize = 44.dp

/**
 * Displays the captured target photo with a calibration circle - always visible, always draggable
 * via two large handles (move at center, resize at the top edge) - plus tap-to-add/drag/long-press
 * arrow marks. There's no separate "calibration mode": the circle starts at [center]/[radiusPx]
 * (auto-detected, previously saved, or a caller-supplied default) and can be adjusted at any time.
 */
@Composable
fun TargetOverlay(
    bitmap: Bitmap,
    center: Offset,
    radiusPx: Float,
    points: List<OverlayPoint>,
    onCenterChange: (Offset) -> Unit,
    onRadiusChange: (Float) -> Unit,
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
        val addOffsetPx = touchOffsetPx(scale, density)

        Box(
            Modifier
                .fillMaxWidth()
                .height(boxHeightDp)
                .pointerInput(points.size, center, radiusPx) {
                    detectTapGestures(onTap = { offset ->
                        val bitmapOffset = Offset(offset.x / scale + addOffsetPx, offset.y / scale)
                        onAddPoint(bitmapOffset)
                    })
                }
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured target",
                modifier = Modifier.fillMaxWidth().height(boxHeightDp),
                contentScale = ContentScale.FillBounds
            )

            Canvas(Modifier.fillMaxWidth().height(boxHeightDp)) {
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = radiusPx * scale,
                    center = Offset(center.x * scale, center.y * scale),
                    style = Stroke(width = 4f)
                )
            }

            CalibrationHandle(
                positionPx = center,
                scale = scale,
                icon = Icons.Filled.OpenWith,
                contentDescription = "Move calibration circle",
                onDrag = onCenterChange
            )
            CalibrationHandle(
                positionPx = Offset(center.x, center.y - radiusPx),
                scale = scale,
                icon = Icons.Filled.ZoomOutMap,
                contentDescription = "Resize calibration circle",
                onDrag = { newPos ->
                    val dx = newPos.x - center.x
                    val dy = newPos.y - center.y
                    onRadiusChange(sqrt(dx * dx + dy * dy))
                }
            )

            points.forEach { point ->
                ArrowMarker(
                    point = MarkerPoint(point.id, point.xPx, point.yPx, point.score, point.isX, point.confirmed),
                    scale = scale,
                    onDrag = { newPxOffset -> onMovePoint(point.id, newPxOffset) },
                    onDelete = { onDeletePoint(point.id) }
                )
            }
        }
    }
}

@Composable
private fun CalibrationHandle(
    positionPx: Offset,
    scale: Float,
    icon: ImageVector,
    contentDescription: String,
    onDrag: (Offset) -> Unit
) {
    val density = LocalDensity.current
    var dragPositionPx by remember { mutableStateOf(positionPx) }
    LaunchedEffect(positionPx) { dragPositionPx = positionPx }

    val handleSizePx = with(density) { HandleSize.toPx() }
    val offset = IntOffset(
        (dragPositionPx.x * scale - handleSizePx / 2).roundToInt(),
        (dragPositionPx.y * scale - handleSizePx / 2).roundToInt()
    )

    Box(
        modifier = Modifier
            .offset { offset }
            .size(HandleSize)
            .shadow(4.dp, CircleShape)
            .background(Color(0xFF1B4F72), CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .pointerInput(Unit) {
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
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White)
    }
}
