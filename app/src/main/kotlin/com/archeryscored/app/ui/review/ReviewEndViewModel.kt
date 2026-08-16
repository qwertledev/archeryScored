package com.archeryscored.app.ui.review

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscored.data.db.entity.ArrowPointEntity
import com.archeryscored.data.repository.SessionRepository
import com.archeryscored.model.MAX_ARROWS_PER_END
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

/** How much of the photo's shorter side a freshly-guessed calibration circle covers by default. */
private const val DEFAULT_RADIUS_FRACTION = 0.4f

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
    val points: List<ReviewPoint> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
) {
    val hasCircle: Boolean get() = centerPx != null && radiusPx != null
    val canAddMore: Boolean get() = points.size < MAX_ARROWS_PER_END
}

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

    /** Called once the photo's pixel dimensions are known, if no real calibration was loaded. */
    fun setDefaultCircleIfNeeded(bitmapWidthPx: Int, bitmapHeightPx: Int) {
        val state = _uiState.value
        if (state.hasCircle) return
        val center = Offset(bitmapWidthPx / 2f, bitmapHeightPx / 2f)
        val radius = minOf(bitmapWidthPx, bitmapHeightPx) * DEFAULT_RADIUS_FRACTION
        _uiState.value = state.copy(centerPx = center, radiusPx = radius)
    }

    fun resetCircle(bitmapWidthPx: Int, bitmapHeightPx: Int) {
        val state = _uiState.value
        val center = Offset(bitmapWidthPx / 2f, bitmapHeightPx / 2f)
        val radius = minOf(bitmapWidthPx, bitmapHeightPx) * DEFAULT_RADIUS_FRACTION
        _uiState.value = state.copy(centerPx = center, radiusPx = radius, points = rescoreAll(state.points, center, radius))
    }

    fun onCenterChange(newCenter: Offset) {
        val state = _uiState.value
        val radius = state.radiusPx ?: return
        _uiState.value = state.copy(centerPx = newCenter, points = rescoreAll(state.points, newCenter, radius))
    }

    fun onRadiusChange(newRadius: Float) {
        val state = _uiState.value
        val center = state.centerPx ?: return
        if (newRadius < 10f) return
        _uiState.value = state.copy(radiusPx = newRadius, points = rescoreAll(state.points, center, newRadius))
    }

    private fun rescoreAll(points: List<ReviewPoint>, center: Offset, radius: Float): List<ReviewPoint> =
        points.map { p ->
            val scored = ScoreCalculator.score(Point2D(p.xPx, p.yPx), Point2D(center.x, center.y), radius, ringConfig)
            p.copy(score = scored.score, isX = scored.isX)
        }

    fun onAddPoint(offset: Offset) {
        val state = _uiState.value
        if (!state.canAddMore) return
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
