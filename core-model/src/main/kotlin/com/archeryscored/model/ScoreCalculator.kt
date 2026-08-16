package com.archeryscored.model

import kotlin.math.sqrt

data class ScoredPoint(val score: Int, val isX: Boolean)

/**
 * Pure ring-distance scoring math, with no Android/OpenCV dependency so it can recompute instantly
 * on every drag frame during manual correction. A point exactly on a ring boundary scores the
 * higher (inner) value, matching the standard archery line-touch rule.
 */
object ScoreCalculator {
    private const val EPSILON = 1e-4f

    fun score(point: Point2D, center: Point2D, faceRadius: Float, ringConfig: RingConfig): ScoredPoint {
        if (faceRadius <= 0f) return ScoredPoint(score = 0, isX = false)
        val dx = point.x - center.x
        val dy = point.y - center.y
        val normalizedDistance = sqrt(dx * dx + dy * dy) / faceRadius
        return scoreNormalizedDistance(normalizedDistance, ringConfig)
    }

    fun scoreNormalized(normalizedPoint: Point2D, ringConfig: RingConfig): ScoredPoint {
        val distance = sqrt(normalizedPoint.x * normalizedPoint.x + normalizedPoint.y * normalizedPoint.y)
        return scoreNormalizedDistance(distance, ringConfig)
    }

    private fun scoreNormalizedDistance(normalizedDistance: Float, ringConfig: RingConfig): ScoredPoint {
        val boundary = ringConfig.sortedBoundaries.firstOrNull { normalizedDistance <= it.outerRadiusRatio + EPSILON }
            ?: return ScoredPoint(score = 0, isX = false)
        val isX = boundary.innerXRadiusRatio?.let { normalizedDistance <= it + EPSILON } ?: false
        return ScoredPoint(score = boundary.score, isX = isX)
    }

    /** Converts a pixel-space point into face-relative normalized coordinates for storage. */
    fun normalize(point: Point2D, center: Point2D, faceRadius: Float): Point2D {
        if (faceRadius <= 0f) return Point2D(0f, 0f)
        return Point2D((point.x - center.x) / faceRadius, (point.y - center.y) / faceRadius)
    }
}
