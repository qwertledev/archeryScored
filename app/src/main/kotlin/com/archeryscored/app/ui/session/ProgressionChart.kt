package com.archeryscored.app.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Simple per-end bar chart with an overlaid cumulative-score line; no charting library needed for a single/dual-series chart like this. */
@Composable
fun ProgressionChart(ends: List<EndSummary>, maxPossiblePerEnd: Int, modifier: Modifier = Modifier) {
    if (ends.isEmpty()) return
    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val barWidth = size.width / ends.size
        val maxBar = maxOf(maxPossiblePerEnd, ends.maxOf { it.score }).toFloat().coerceAtLeast(1f)

        ends.forEachIndexed { index, end ->
            val barHeight = (end.score / maxBar) * size.height
            drawRect(
                color = Color(0xFF1976D2),
                topLeft = Offset(index * barWidth + barWidth * 0.15f, size.height - barHeight),
                size = Size(barWidth * 0.7f, barHeight)
            )
        }

        val cumulative = ends.runningFold(0) { acc, e -> acc + e.score }.drop(1)
        val maxCumulative = cumulative.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
        val path = Path()
        cumulative.forEachIndexed { index, total ->
            val x = index * barWidth + barWidth / 2f
            val y = size.height - (total / maxCumulative) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = Color(0xFFE53935), style = Stroke(width = 4f))
    }
}
