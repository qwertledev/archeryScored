package com.archeryscored.app.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.archeryscored.app.ui.common.colorFor
import com.archeryscored.model.Point2D
import com.archeryscored.model.RingConfig

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
                color = colorFor(boundary.colorHint),
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
