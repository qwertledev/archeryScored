package com.archeryscored.cv.arrows

import android.graphics.Bitmap
import com.archeryscored.cv.geometry.GeometryDetectionResult
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
import kotlin.math.PI
import kotlin.math.sqrt

data class DetectedArrow(val xPx: Float, val yPx: Float, val confidence: Float)

/**
 * Finds small dark/high-contrast circular blobs (arrow holes) within the calibrated face area.
 * Expected hole size is derived from real arrow-shaft diameter and the pixels-per-cm implied by
 * calibration, rather than a hardcoded pixel size, so it scales across face sizes/photo resolutions.
 *
 * Like [com.archeryscored.cv.geometry.TargetFaceDetector], this is unvalidated against real photos -
 * every result must be treated as a suggestion for the manual review screen to confirm or correct.
 * Known blind spots: touching/overlapping holes, tape-patched old holes, low contrast on the black
 * ring - all accepted MVP limitations handled by manual correction, not solved here.
 */
object ArrowHoleDetector {

    private const val MIN_ARROW_SHAFT_MM = 5.0
    private const val MAX_ARROW_SHAFT_MM = 9.5
    private const val MIN_CIRCULARITY = 0.5
    private const val FACE_MASK_SHRINK_FACTOR = 0.97

    fun detect(bitmap: Bitmap, geometry: GeometryDetectionResult, faceDiameterCm: Float): List<DetectedArrow> {
        val rgba = Mat()
        Utils.bitmapToMat(bitmap, rgba)
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        rgba.release()

        val pixelsPerCm = geometry.radiusPx / (faceDiameterCm / 2f)
        val minRadiusPx = pixelsPerCm * (MIN_ARROW_SHAFT_MM / 10.0) / 2.0
        val maxRadiusPx = pixelsPerCm * (MAX_ARROW_SHAFT_MM / 10.0) / 2.0 * 1.6
        val minArea = PI * minRadiusPx * minRadiusPx
        val maxArea = PI * maxRadiusPx * maxRadiusPx

        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(3.0, 3.0), 0.0)
        gray.release()

        val thresh = Mat()
        Imgproc.adaptiveThreshold(
            blurred, thresh, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV,
            25, 5.0
        )
        blurred.release()

        val faceMask = Mat.zeros(thresh.size(), CvType.CV_8UC1)
        Imgproc.circle(
            faceMask,
            Point(geometry.centerXPx.toDouble(), geometry.centerYPx.toDouble()),
            (geometry.radiusPx * FACE_MASK_SHRINK_FACTOR).toInt(),
            Scalar(255.0),
            -1
        )
        Core.bitwise_and(thresh, faceMask, thresh)
        faceMask.release()

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(thresh, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        thresh.release()

        val candidates = mutableListOf<DetectedArrow>()
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area < minArea || area > maxArea) continue

            val points2f = MatOfPoint2f(*contour.toArray())
            val perimeter = Imgproc.arcLength(points2f, true)
            points2f.release()
            if (perimeter <= 0) continue

            val circularity = 4 * PI * area / (perimeter * perimeter)
            if (circularity < MIN_CIRCULARITY) continue

            val moments = Imgproc.moments(contour)
            if (moments.m00 == 0.0) continue
            val cx = (moments.m10 / moments.m00).toFloat()
            val cy = (moments.m01 / moments.m00).toFloat()
            candidates.add(DetectedArrow(cx, cy, circularity.toFloat().coerceIn(0f, 1f)))
        }

        return dedupe(candidates, minSpacingPx = (minRadiusPx * 1.5).toFloat())
    }

    private fun dedupe(points: List<DetectedArrow>, minSpacingPx: Float): List<DetectedArrow> {
        val kept = mutableListOf<DetectedArrow>()
        for (candidate in points.sortedByDescending { it.confidence }) {
            val tooClose = kept.any { existing ->
                val dx = existing.xPx - candidate.xPx
                val dy = existing.yPx - candidate.yPx
                sqrt((dx * dx + dy * dy).toDouble()) < minSpacingPx
            }
            if (!tooClose) kept.add(candidate)
        }
        return kept
    }
}
