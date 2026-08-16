package com.archeryscored.model

/** Predefined target faces available when starting a session. */
object TargetFaces {

    /** Standard World Archery 10-zone face: 10 equal-width rings, X-ring = inner half of the 10-ring. */
    val WA_10_ZONE_RING_CONFIG = RingConfig(
        listOf(
            RingBoundary(score = 10, outerRadiusRatio = 0.10f, colorHint = RingColor.GOLD, innerXRadiusRatio = 0.05f),
            RingBoundary(score = 9, outerRadiusRatio = 0.20f, colorHint = RingColor.GOLD),
            RingBoundary(score = 8, outerRadiusRatio = 0.30f, colorHint = RingColor.RED),
            RingBoundary(score = 7, outerRadiusRatio = 0.40f, colorHint = RingColor.RED),
            RingBoundary(score = 6, outerRadiusRatio = 0.50f, colorHint = RingColor.BLUE),
            RingBoundary(score = 5, outerRadiusRatio = 0.60f, colorHint = RingColor.BLUE),
            RingBoundary(score = 4, outerRadiusRatio = 0.70f, colorHint = RingColor.BLACK),
            RingBoundary(score = 3, outerRadiusRatio = 0.80f, colorHint = RingColor.BLACK),
            RingBoundary(score = 2, outerRadiusRatio = 0.90f, colorHint = RingColor.WHITE),
            RingBoundary(score = 1, outerRadiusRatio = 1.00f, colorHint = RingColor.WHITE)
        )
    )

    /**
     * Approximate 6-ring field face layout. NOT verified against the official World Archery field
     * rulebook - confirm exact ring widths/colors before relying on this for scored field rounds.
     */
    val FIELD_6_RING_CONFIG = RingConfig(
        listOf(
            RingBoundary(score = 6, outerRadiusRatio = 1f / 6f, colorHint = RingColor.GOLD),
            RingBoundary(score = 5, outerRadiusRatio = 2f / 6f, colorHint = RingColor.RED),
            RingBoundary(score = 4, outerRadiusRatio = 3f / 6f, colorHint = RingColor.BLUE),
            RingBoundary(score = 3, outerRadiusRatio = 4f / 6f, colorHint = RingColor.BLUE),
            RingBoundary(score = 2, outerRadiusRatio = 5f / 6f, colorHint = RingColor.BLACK),
            RingBoundary(score = 1, outerRadiusRatio = 1.00f, colorHint = RingColor.BLACK)
        )
    )

    val WA_40CM = TargetFaceType("wa_40cm", "WA 40cm (Indoor)", 40f, WA_10_ZONE_RING_CONFIG, indoor = true)
    val WA_60CM = TargetFaceType("wa_60cm", "WA 60cm (Indoor)", 60f, WA_10_ZONE_RING_CONFIG, indoor = true)
    val WA_80CM = TargetFaceType("wa_80cm", "WA 80cm (Outdoor)", 80f, WA_10_ZONE_RING_CONFIG, indoor = false)
    val WA_122CM = TargetFaceType("wa_122cm", "WA 122cm (Outdoor)", 122f, WA_10_ZONE_RING_CONFIG, indoor = false)
    val FIELD_FACE = TargetFaceType("field_6ring", "Field 6-Ring (approx.)", 40f, FIELD_6_RING_CONFIG, indoor = false)
    val INDOOR_3_SPOT = TargetFaceType("indoor_3spot", "Indoor 40cm 3-Spot", 40f, WA_10_ZONE_RING_CONFIG, indoor = true, spotCount = 3)

    val all: List<TargetFaceType> = listOf(WA_40CM, WA_60CM, WA_80CM, WA_122CM, FIELD_FACE, INDOOR_3_SPOT)

    /** The four standard single-spot WA recurve face sizes, split by indoor/outdoor - what the New Session picker offers. */
    val standardRecurve: List<TargetFaceType> = listOf(WA_40CM, WA_60CM, WA_80CM, WA_122CM)

    fun byId(id: String): TargetFaceType = all.first { it.id == id }
}
