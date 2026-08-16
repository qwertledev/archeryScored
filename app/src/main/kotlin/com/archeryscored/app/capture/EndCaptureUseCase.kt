package com.archeryscored.app.capture

import android.graphics.BitmapFactory
import com.archeryscored.cv.DetectionPipeline
import com.archeryscored.data.db.entity.ArrowPointEntity
import com.archeryscored.data.repository.SessionRepository
import com.archeryscored.model.Point2D
import com.archeryscored.model.PointSource
import com.archeryscored.model.ScoreCalculator
import com.archeryscored.model.TargetFaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.File
import javax.inject.Inject

/**
 * Shared "a photo file exists for this end, now record it" pipeline - used by both the camera
 * capture path and the gallery-upload path, since from this point on they're identical.
 */
class EndCaptureUseCase @Inject constructor(
    private val repository: SessionRepository
) {
    suspend fun persistEnd(sessionId: Long, endNumber: Int, file: File): Long {
        val endId = repository.createEnd(sessionId, endNumber, file, Clock.System.now())
        runCatching { runAutoDetection(sessionId, endId, file) }
        return endId
    }

    /**
     * Best-effort: pre-fills calibration + arrow points from the cv module so Review opens
     * ready to correct rather than empty. Any failure (low confidence, unknown face type,
     * OpenCV native error) just leaves the end uncalibrated - Review then starts the manual
     * two-tap calibration flow instead. Auto-detection is a suggestion, never a dependency.
     */
    private suspend fun runAutoDetection(sessionId: Long, endId: Long, file: File) {
        val session = repository.getSession(sessionId).first() ?: return
        val face = runCatching { TargetFaces.byId(session.targetFaceTypeId) }.getOrNull() ?: return
        val bitmap = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.absolutePath) } ?: return
        val outcome = withContext(Dispatchers.Default) {
            DetectionPipeline.run(bitmap, face.ringConfig, face.faceDiameterCm)
        } ?: return

        val geometry = outcome.geometry
        repository.updateCalibration(endId, geometry.centerXPx, geometry.centerYPx, geometry.radiusPx, confirmed = false)

        val points = outcome.arrows.map { arrow ->
            val normalized = ScoreCalculator.normalize(
                Point2D(arrow.xPx, arrow.yPx),
                Point2D(geometry.centerXPx, geometry.centerYPx),
                geometry.radiusPx
            )
            val scored = ScoreCalculator.scoreNormalized(normalized, face.ringConfig)
            ArrowPointEntity(
                endId = endId,
                xNormalized = normalized.x,
                yNormalized = normalized.y,
                score = scored.score,
                isX = scored.isX,
                source = PointSource.AUTO_DETECTED
            )
        }
        repository.saveArrowPoints(endId, points)
    }
}
