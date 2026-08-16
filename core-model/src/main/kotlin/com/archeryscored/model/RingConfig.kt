package com.archeryscored.model

/**
 * One scoring ring: everything from the previous boundary's [outerRadiusRatio] out to this one's
 * scores [score]. [outerRadiusRatio] is expressed as a fraction of the full face radius (1.0 = outer edge).
 * [innerXRadiusRatio], only set on the highest-scoring ring, marks the inner "X" ring used for tie-breaks.
 */
data class RingBoundary(
    val score: Int,
    val outerRadiusRatio: Float,
    val colorHint: RingColor,
    val innerXRadiusRatio: Float? = null
)

/**
 * Ordered ring geometry + scoring for a target face. The same config drives both [ScoreCalculator]
 * and the CV color-search sequence in the `cv` module, so a new face type only needs new config here.
 */
data class RingConfig(val boundaries: List<RingBoundary>) {
    init {
        require(boundaries.isNotEmpty()) { "RingConfig must declare at least one ring boundary" }
    }

    val sortedBoundaries: List<RingBoundary> = boundaries.sortedBy { it.outerRadiusRatio }
    val maxScore: Int = boundaries.maxOf { it.score }
}
