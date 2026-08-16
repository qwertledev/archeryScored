package com.archeryscored.model

import kotlin.math.sqrt

/** [groupRadius] is the radius of the smallest circle centered on [meanPoint] that contains every arrow, in normalized units. */
data class GroupStats(val meanPoint: Point2D, val groupRadius: Float, val arrowCount: Int)

object GroupStatsCalculator {
    fun compute(points: List<Point2D>): GroupStats? {
        if (points.isEmpty()) return null
        val meanX = points.sumOf { it.x.toDouble() }.toFloat() / points.size
        val meanY = points.sumOf { it.y.toDouble() }.toFloat() / points.size
        val mean = Point2D(meanX, meanY)
        val maxDist = points.maxOf { p ->
            val dx = p.x - mean.x
            val dy = p.y - mean.y
            sqrt(dx * dx + dy * dy)
        }
        return GroupStats(meanPoint = mean, groupRadius = maxDist, arrowCount = points.size)
    }
}
