package com.archeryscored.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.archeryscored.model.RingConfig

data class DiagramPoint(val id: Long, val xNormalized: Float, val yNormalized: Float, val score: Int, val isX: Boolean)

/**
 * A blank target face the archer taps directly to mark where each arrow landed - no photo involved.
 * Reports/accepts positions already normalized to the diagram's own center/radius (-1..1), so the
 * caller never needs to think in pixels.
 */
@Composable
fun TargetDiagramInput(
    ringConfig: RingConfig,
    points: List<DiagramPoint>,
    onAddPoint: (Offset) -> Unit,
    onMovePoint: (id: Long, normalized: Offset) -> Unit,
    onDeletePoint: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        val density = LocalDensity.current
        val radiusPx = with(density) { maxWidth.toPx() } / 2f
        val centerPx = Offset(radiusPx, radiusPx)
        val addOffsetPx = touchOffsetPx(scale = 1f, density = density)

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(points.size) {
                    detectTapGestures(onTap = { raw ->
                        val shifted = Offset(raw.x + addOffsetPx, raw.y)
                        val normalized = Offset((shifted.x - centerPx.x) / radiusPx, (shifted.y - centerPx.y) / radiusPx)
                        onAddPoint(normalized)
                    })
                }
        ) {
            drawTargetFace(ringConfig = ringConfig, center = centerPx, radiusPx = radiusPx, showLabels = true)
        }

        points.forEach { p ->
            val xPx = centerPx.x + p.xNormalized * radiusPx
            val yPx = centerPx.y + p.yNormalized * radiusPx
            ArrowMarker(
                point = MarkerPoint(p.id, xPx, yPx, p.score, p.isX, confirmed = true),
                scale = 1f,
                onDrag = { newPxPos ->
                    val normalized = Offset((newPxPos.x - centerPx.x) / radiusPx, (newPxPos.y - centerPx.y) / radiusPx)
                    onMovePoint(p.id, normalized)
                },
                onDelete = { onDeletePoint(p.id) }
            )
        }
    }
}
