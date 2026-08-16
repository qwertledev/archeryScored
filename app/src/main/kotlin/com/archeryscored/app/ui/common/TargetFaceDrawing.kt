package com.archeryscored.app.ui.common

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.archeryscored.model.RingColor
import com.archeryscored.model.RingConfig

/**
 * Draws a target face with every individual scoring ring visually distinct - not just the 5 fill
 * colors, since e.g. the 10 and 9 rings share GOLD and would otherwise blend into one solid blob
 * with no visible seam. Adds a thin boundary stroke between every ring (light on dark fills, dark
 * on light fills) and, optionally, the score number centered in each band.
 */
fun DrawScope.drawTargetFace(
    ringConfig: RingConfig,
    center: Offset,
    radiusPx: Float,
    showLabels: Boolean = false
) {
    val boundaries = ringConfig.sortedBoundaries

    boundaries.sortedByDescending { it.outerRadiusRatio }.forEach { boundary ->
        drawCircle(color = colorFor(boundary.colorHint), radius = radiusPx * boundary.outerRadiusRatio, center = center)
    }

    val strokeWidthPx = 1.5.dp.toPx()
    boundaries.forEach { boundary ->
        val strokeColor = if (boundary.colorHint == RingColor.BLACK) {
            Color.White.copy(alpha = 0.75f)
        } else {
            Color.Black.copy(alpha = 0.55f)
        }
        drawCircle(
            color = strokeColor,
            radius = radiusPx * boundary.outerRadiusRatio,
            center = center,
            style = Stroke(width = strokeWidthPx)
        )
    }

    if (showLabels) {
        val textSizePx = radiusPx * 0.09f
        val paint = Paint().apply {
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            textSize = textSizePx
        }
        var innerRatio = 0f
        boundaries.forEach { boundary ->
            val midRatio = (innerRatio + boundary.outerRadiusRatio) / 2f
            val labelColor = if (boundary.colorHint == RingColor.BLACK || boundary.colorHint == RingColor.BLUE) {
                Color.White
            } else {
                Color.Black
            }
            paint.color = labelColor.toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                boundary.score.toString(),
                center.x,
                center.y - radiusPx * midRatio + textSizePx / 3f,
                paint
            )
            innerRatio = boundary.outerRadiusRatio
        }
    }
}
