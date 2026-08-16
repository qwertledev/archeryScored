package com.archeryscored.model

/**
 * A selectable target face. [spotCount] > 1 means the archer shoots at multiple separate faces per end
 * (e.g. indoor 3-spot); automatic multi-spot detection is not implemented yet, so those face types
 * currently rely on manual calibration/placement per spot.
 */
data class TargetFaceType(
    val id: String,
    val displayName: String,
    val faceDiameterCm: Float,
    val ringConfig: RingConfig,
    val indoor: Boolean,
    val spotCount: Int = 1
)
