package com.archeryscored.model

/** A simple 2D point. In pixel-space contexts, x/y are pixels; in normalized contexts, both are in units of face radius relative to the target center. */
data class Point2D(val x: Float, val y: Float)
