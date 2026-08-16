package com.archeryscored.app.ui.capture

import android.graphics.BitmapFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscored.cv.DetectionPipeline
import com.archeryscored.data.db.entity.ArrowPointEntity
import com.archeryscored.data.repository.SessionRepository
import com.archeryscored.model.Point2D
import com.archeryscored.model.PointSource
import com.archeryscored.model.ScoreCalculator
import com.archeryscored.model.TargetFaces
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CaptureEndViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository
) : ViewModel() {

    val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    // Derived from the DB rather than a local counter, so it stays correct even though this
    // ViewModel is recreated fresh each time the archer returns here after finishing an end.
    val endCount: StateFlow<Int> = repository.getEndsForSession(sessionId)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _navigateToReview = MutableStateFlow<Long?>(null)
    val navigateToReview: StateFlow<Long?> = _navigateToReview.asStateFlow()

    fun newPhotoFile(): File {
        val endNumber = endCount.value + 1
        return repository.newPhotoFile(sessionId, endNumber)
    }

    fun onPhotoSaved(file: File) {
        viewModelScope.launch {
            _isSaving.value = true
            val endNumber = endCount.value + 1
            val endId = repository.createEnd(sessionId, endNumber, file, Clock.System.now())
            runCatching { runAutoDetection(endId, file) }
            _isSaving.value = false
            _navigateToReview.value = endId
        }
    }

    /**
     * Best-effort: pre-fills calibration + arrow points from the cv module so Review opens
     * ready to correct rather than empty. Any failure (low confidence, unknown face type,
     * OpenCV native error) just leaves the end uncalibrated - Review then starts the manual
     * two-tap calibration flow instead. Auto-detection is a suggestion, never a dependency.
     */
    private suspend fun runAutoDetection(endId: Long, file: File) {
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

    fun consumeNavigation() {
        _navigateToReview.value = null
    }
}
