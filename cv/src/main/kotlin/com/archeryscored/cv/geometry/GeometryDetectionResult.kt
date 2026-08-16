package com.archeryscored.cv.geometry

data class GeometryDetectionResult(
    val centerXPx: Float,
    val centerYPx: Float,
    val radiusPx: Float,
    val confidence: Float,
    val ringsDetected: Int,
    val ringsExpected: Int
)
