package com.archeryscored.app.ui.review

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscored.data.db.entity.ArrowPointEntity
import com.archeryscored.data.repository.SessionRepository
import com.archeryscored.model.Point2D
import com.archeryscored.model.PointSource
import com.archeryscored.model.RingConfig
import com.archeryscored.model.ScoreCalculator
import com.archeryscored.model.TargetFaces
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

data class ReviewPoint(
    val id: Long,
    val xPx: Float,
    val yPx: Float,
    val score: Int,
    val isX: Boolean,
    val source: PointSource
)

data class ReviewUiState(
    val photoPath: String? = null,
    val centerPx: Offset? = null,
    val radiusPx: Float? = null,
    val mode: OverlayMode = OverlayMode.TAP_CENTER,
    val points: List<ReviewPoint> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)

@HiltViewModel
class ReviewEndViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository
) : ViewModel() {

    val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])
    private val endId: Long = checkNotNull(savedStateHandle["endId"])

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private var nextLocalId = -1L
    private var ringConfig: RingConfig = TargetFaces.WA_122CM.ringConfig

    init {
        viewModelScope.launch {
            val session = repository.getSession(sessionId).first()
            val end = repository.getEndsForSession(sessionId).first().first { it.id == endId }
            val face = session?.let { runCatching { TargetFaces.byId(it.targetFaceTypeId) }.getOrNull() }
                ?: TargetFaces.WA_122CM
            ringConfig = face.ringConfig

            val existingPoints = repository.getPointsForEnd(endId).first()
            val hasCalibration = end.centerXPx != null && end.radiusPx != null
            val center = if (hasCalibration) Offset(end.centerXPx!!, end.centerYPx!!) else null
            val radius = end.radiusPx

            _uiState.value = ReviewUiState(
                photoPath = end.photoPath,
                centerPx = center,
                radiusPx = radius,
                mode = if (hasCalibration) OverlayMode.PLACE_POINTS else OverlayMode.TAP_CENTER,
                points = existingPoints.mapNotNull { p ->
                    val x = p.xNormalized ?: return@mapNotNull null
                    val y = p.yNormalized ?: return@mapNotNull null
                    val px = denormalize(Point2D(x, y), center, radius)
                    ReviewPoint(p.id, px.x, px.y, p.score, p.isX, p.source)
                },
                isLoading = false
            )
        }
    }

    private fun denormalize(normalized: Point2D, center: Offset?, radius: Float?): Point2D {
        if (center == null || radius == null) return normalized
        return Point2D(center.x + normalized.x * radius, center.y + normalized.y * radius)
    }

    fun onCalibrationTap(offset: Offset) {
        val state = _uiState.value
        when (state.mode) {
            OverlayMode.TAP_CENTER -> {
                _uiState.value = state.copy(centerPx = offset, mode = OverlayMode.TAP_EDGE)
            }
            OverlayMode.TAP_EDGE -> {
                val center = state.centerPx ?: return
                val dx = offset.x - center.x
                val dy = offset.y - center.y
                val radius = sqrt(dx * dx + dy * dy)
                if (radius < 1f) return
                _uiState.value = state.copy(radiusPx = radius, mode = OverlayMode.PLACE_POINTS)
            }
            OverlayMode.PLACE_POINTS -> Unit
        }
    }

    fun onRecalibrate() {
        _uiState.value = _uiState.value.copy(centerPx = null, radiusPx = null, mode = OverlayMode.TAP_CENTER)
    }

    fun onAddPoint(offset: Offset) {
        val state = _uiState.value
        val center = state.centerPx ?: return
        val radius = state.radiusPx ?: return
        val scored = ScoreCalculator.score(Point2D(offset.x, offset.y), Point2D(center.x, center.y), radius, ringConfig)
        val newPoint = ReviewPoint(nextLocalId--, offset.x, offset.y, scored.score, scored.isX, PointSource.MANUAL_ADDED)
        _uiState.value = state.copy(points = state.points + newPoint)
    }

    fun onMovePoint(id: Long, newPosition: Offset) {
        val state = _uiState.value
        val center = state.centerPx ?: return
        val radius = state.radiusPx ?: return
        val scored = ScoreCalculator.score(Point2D(newPosition.x, newPosition.y), Point2D(center.x, center.y), radius, ringConfig)
        _uiState.value = state.copy(
            points = state.points.map {
                if (it.id == id) {
                    it.copy(xPx = newPosition.x, yPx = newPosition.y, score = scored.score, isX = scored.isX, source = PointSource.MANUAL_ADJUSTED)
                } else it
            }
        )
    }

    fun onDeletePoint(id: Long) {
        _uiState.value = _uiState.value.copy(points = _uiState.value.points.filterNot { it.id == id })
    }

    fun save() {
        val state = _uiState.value
        val center = state.centerPx ?: return
        val radius = state.radiusPx ?: return
        viewModelScope.launch {
            repository.updateCalibration(endId, center.x, center.y, radius, confirmed = true)
            val entities = state.points.map { p ->
                val normalized = ScoreCalculator.normalize(Point2D(p.xPx, p.yPx), Point2D(center.x, center.y), radius)
                ArrowPointEntity(
                    endId = endId,
                    xNormalized = normalized.x,
                    yNormalized = normalized.y,
                    score = p.score,
                    isX = p.isX,
                    source = p.source
                )
            }
            repository.saveArrowPoints(endId, entities)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
