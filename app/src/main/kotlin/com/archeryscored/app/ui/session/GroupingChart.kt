package com.archeryscored.app.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.archeryscored.model.Point2D
import com.archeryscored.model.RingColor
import com.archeryscored.model.RingConfig

private val ringColorMap = mapOf(
    RingColor.GOLD to Color(0xFFF4C430),
    RingColor.RED to Color(0xFFD32F2F),
    RingColor.BLUE to Color(0xFF1976D2),
    RingColor.BLACK to Color(0xFF212121),
    RingColor.WHITE to Color(0xFFF5F5F5)
)

/** Draws a simplified target face from [ringConfig] and overlays every session arrow position (normalized). */
@Composable
fun GroupingChart(
    ringConfig: RingConfig,
    points: List<Point2D>,
    meanPoint: Point2D?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        val radiusPx = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        ringConfig.sortedBoundaries.sortedByDescending { it.outerRadiusRatio }.forEach { boundary ->
            drawCircle(
                color = ringColorMap[boundary.colorHint] ?: Color.Gray,
                radius = radiusPx * boundary.outerRadiusRatio,
                center = center
            )
        }
        drawCircle(color = Color.Black, radius = radiusPx, center = center, style = Stroke(width = 2f))

        points.forEach { p ->
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 6f,
                center = Offset(center.x + p.x * radiusPx, center.y + p.y * radiusPx),
                style = Stroke(width = 3f)
            )
        }

        meanPoint?.let { mp ->
            val c = Offset(center.x + mp.x * radiusPx, center.y + mp.y * radiusPx)
            drawLine(Color.Magenta, Offset(c.x - 12f, c.y), Offset(c.x + 12f, c.y), strokeWidth = 3f)
            drawLine(Color.Magenta, Offset(c.x, c.y - 12f), Offset(c.x, c.y + 12f), strokeWidth = 3f)
        }
    }
}
