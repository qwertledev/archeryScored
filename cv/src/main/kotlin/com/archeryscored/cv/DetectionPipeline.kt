package com.archeryscored.cv

import android.graphics.Bitmap
import com.archeryscored.cv.arrows.ArrowHoleDetector
import com.archeryscored.cv.arrows.DetectedArrow
import com.archeryscored.cv.geometry.GeometryDetectionResult
import com.archeryscored.cv.geometry.TargetFaceDetector
import com.archeryscored.model.RingConfig

data class DetectionOutcome(
    val geometry: GeometryDetectionResult,
    val arrows: List<DetectedArrow>
)

/**
 * Runs the full best-effort auto-detection pipeline for one end's photo. Returns null if geometry
 * couldn't be located with sufficient confidence, or if anything throws - OpenCV native failures
 * must never crash the app, they should just fall back to the manual calibration/placement flow.
 * Call off the main thread; this does real image processing work.
 */
object DetectionPipeline {
    fun run(bitmap: Bitmap, ringConfig: RingConfig, faceDiameterCm: Float): DetectionOutcome? {
        if (!OpenCvBootstrap.ensureLoaded()) return null
        val result = runCatching {
            val geometry = TargetFaceDetector.detect(bitmap, ringConfig) ?: return@runCatching null
            val arrows = ArrowHoleDetector.detect(bitmap, geometry, faceDiameterCm)
            DetectionOutcome(geometry, arrows)
        }
        return result.getOrNull()
    }
}
