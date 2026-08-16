package com.archeryscored.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreCalculatorTest {

    private val config = TargetFaces.WA_10_ZONE_RING_CONFIG
    private val center = Point2D(500f, 500f)
    private val radius = 400f

    @Test
    fun `dead center scores 10 and X`() {
        val result = ScoreCalculator.score(center, center, radius, config)
        assertEquals(10, result.score)
        assertTrue(result.isX)
    }

    @Test
    fun `just outside the X ring scores 10 but not X`() {
        val point = Point2D(center.x + radius * 0.06f, center.y)
        val result = ScoreCalculator.score(point, center, radius, config)
        assertEquals(10, result.score)
        assertFalse(result.isX)
    }

    @Test
    fun `point exactly on a ring boundary scores the higher inner value`() {
        val point = Point2D(center.x + radius * 0.20f, center.y)
        val result = ScoreCalculator.score(point, center, radius, config)
        assertEquals(9, result.score)
    }

    @Test
    fun `point beyond the outer edge scores zero`() {
        val point = Point2D(center.x + radius * 1.5f, center.y)
        val result = ScoreCalculator.score(point, center, radius, config)
        assertEquals(0, result.score)
    }

    @Test
    fun `normalize then scoreNormalized matches direct score`() {
        val point = Point2D(center.x + 123f, center.y - 77f)
        val direct = ScoreCalculator.score(point, center, radius, config)
        val normalized = ScoreCalculator.normalize(point, center, radius)
        val viaNormalized = ScoreCalculator.scoreNormalized(normalized, config)
        assertEquals(direct, viaNormalized)
    }

    @Test
    fun `zero radius never crashes and scores zero`() {
        val result = ScoreCalculator.score(Point2D(1f, 1f), center, 0f, config)
        assertEquals(0, result.score)
    }
}
