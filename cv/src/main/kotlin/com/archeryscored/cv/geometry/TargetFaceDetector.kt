package com.archeryscored.cv.geometry

import android.graphics.Bitmap
import com.archeryscored.model.RingColor
import com.archeryscored.model.RingConfig
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Locates the target face's ring geometry (center + radius, in the ORIGINAL bitmap's pixel space)
 * via HSV color segmentation + circle fitting: one pass per expected ring color, then a consensus
 * across however many rings were found, since all rings are concentric with known radius ratios.
 *
 * This is a best-effort classical CV pipeline (no training data exists for a learned model) and
 * has NOT been tuned against a corpus of real target photos - see tools/cv-harness for the offline
 * tuning workflow this needs before being trusted. Always confidence-gate the result and fall back
 * to manual calibration when it's null or low-confidence; never treat this as ground truth.
 */
object TargetFaceDetector {

    private const val WORKING_MAX_DIMENSION = 1200.0
    private const val MIN_CONFIDENCE = 0.5f
    private const val MIN_CONTOUR_AREA = 200.0

    private val hsvRanges: Map<RingColor, List<Pair<Scalar, Scalar>>> = mapOf(
        RingColor.GOLD to listOf(Scalar(15.0, 80.0, 120.0) to Scalar(35.0, 255.0, 255.0)),
        RingColor.RED to listOf(
            Scalar(0.0, 90.0, 60.0) to Scalar(10.0, 255.0, 255.0),
            Scalar(170.0, 90.0, 60.0) to Scalar(180.0, 255.0, 255.0)
        ),
        RingColor.BLUE to listOf(Scalar(90.0, 60.0, 60.0) to Scalar(130.0, 255.0, 255.0)),
        RingColor.BLACK to listOf(Scalar(0.0, 0.0, 0.0) to Scalar(180.0, 120.0, 60.0)),
        RingColor.WHITE to listOf(Scalar(0.0, 0.0, 170.0) to Scalar(180.0, 60.0, 255.0))
    )

    fun detect(bitmap: Bitmap, ringConfig: RingConfig): GeometryDetectionResult? {
        val argb = Mat()
        Utils.bitmapToMat(bitmap, argb)
        val bgr = Mat()
        Imgproc.cvtColor(argb, bgr, Imgproc.COLOR_RGBA2BGR)
        argb.release()

        val longestSide = maxOf(bgr.width(), bgr.height()).toDouble()
        val scale = if (longestSide > WORKING_MAX_DIMENSION) WORKING_MAX_DIMENSION / longestSide else 1.0
        val working = Mat()
        if (scale < 1.0) {
            Imgproc.resize(bgr, working, Size(bgr.width() * scale, bgr.height() * scale))
        } else {
            bgr.copyTo(working)
        }
        val inverseScale = 1.0 / scale
        bgr.release()

        val hsv = Mat()
        Imgproc.cvtColor(working, hsv, Imgproc.COLOR_BGR2HSV)
        working.release()

        val expectedColors = ringConfig.sortedBoundaries.map { it.colorHint }.distinct()
        val impliedCenterX = mutableListOf<Double>()
        val impliedCenterY = mutableListOf<Double>()
        val impliedRadius = mutableListOf<Double>()

        for (color in expectedColors) {
            val ranges = hsvRanges[color] ?: continue
            val boundaryRatio = ringConfig.sortedBoundaries
                .filter { it.colorHint == color }
                .maxOfOrNull { it.outerRadiusRatio }
                ?: continue
            if (boundaryRatio <= 0f) continue

            val mask = Mat.zeros(hsv.size(), CvType.CV_8UC1)
            for ((lower, upper) in ranges) {
                val partial = Mat()
                Core.inRange(hsv, lower, upper, partial)
                Core.bitwise_or(mask, partial, mask)
                partial.release()
            }
            Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, Mat())
            Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, Mat())

            val contours = mutableListOf<MatOfPoint>()
            Imgproc.findContours(mask, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            mask.release()

            val best = contours.maxByOrNull { Imgproc.contourArea(it) }
            if (best != null && Imgproc.contourArea(best) >= MIN_CONTOUR_AREA) {
                val points2f = MatOfPoint2f(*best.toArray())
                val center = Point()
                val radiusHolder = FloatArray(1)
                Imgproc.minEnclosingCircle(points2f, center, radiusHolder)
                points2f.release()

                if (radiusHolder[0] > 0f) {
                    impliedCenterX.add(center.x * inverseScale)
                    impliedCenterY.add(center.y * inverseScale)
                    impliedRadius.add(radiusHolder[0] * inverseScale / boundaryRatio)
                }
            }
        }

        hsv.release()

        if (impliedCenterX.isEmpty()) return null

        val confidence = (impliedCenterX.size.toFloat() / expectedColors.size.toFloat()).coerceIn(0f, 1f)
        val result = GeometryDetectionResult(
            centerXPx = median(impliedCenterX).toFloat(),
            centerYPx = median(impliedCenterY).toFloat(),
            radiusPx = median(impliedRadius).toFloat(),
            confidence = confidence,
            ringsDetected = impliedCenterX.size,
            ringsExpected = expectedColors.size
        )
        return result.takeIf { it.confidence >= MIN_CONFIDENCE }
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        return sorted[sorted.size / 2]
    }
}
