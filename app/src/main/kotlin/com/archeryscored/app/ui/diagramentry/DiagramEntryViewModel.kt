package com.archeryscored.app.ui.diagramentry

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

data class DiagramPoint(val id: Long, val xNormalized: Float, val yNormalized: Float, val score: Int, val isX: Boolean)

data class DiagramEntryUiState(
    val ringConfig: RingConfig = TargetFaces.WA_122CM.ringConfig,
    val points: List<DiagramPoint> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false
) {
    val totalScore: Int get() = points.sumOf { it.score }
}

@HiltViewModel
class DiagramEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository
) : ViewModel() {

    val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val endCount: StateFlow<Int> = repository.getEndsForSession(sessionId)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var nextLocalId = -1L
    private val _uiState = MutableStateFlow(DiagramEntryUiState())
    val uiState: StateFlow<DiagramEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = repository.getSession(sessionId).first()
            val face = session?.let { runCatching { TargetFaces.byId(it.targetFaceTypeId) }.getOrNull() }
            if (face != null) {
                _uiState.value = _uiState.value.copy(ringConfig = face.ringConfig)
            }
        }
    }

    fun onAddPoint(normalized: Offset) {
        val state = _uiState.value
        val scored = ScoreCalculator.scoreNormalized(Point2D(normalized.x, normalized.y), state.ringConfig)
        val point = DiagramPoint(nextLocalId--, normalized.x, normalized.y, scored.score, scored.isX)
        _uiState.value = state.copy(points = state.points + point)
    }

    fun onMovePoint(id: Long, normalized: Offset) {
        val state = _uiState.value
        val scored = ScoreCalculator.scoreNormalized(Point2D(normalized.x, normalized.y), state.ringConfig)
        _uiState.value = state.copy(
            points = state.points.map {
                if (it.id == id) it.copy(xNormalized = normalized.x, yNormalized = normalized.y, score = scored.score, isX = scored.isX)
                else it
            }
        )
    }

    fun onDeletePoint(id: Long) {
        _uiState.value = _uiState.value.copy(points = _uiState.value.points.filterNot { it.id == id })
    }

    fun saveEnd() {
        val state = _uiState.value
        if (state.points.isEmpty() || state.isSaving) return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            val endNumber = endCount.value + 1
            val endId = repository.createEnd(sessionId, endNumber, null, Clock.System.now())
            val entities = state.points.map { p ->
                ArrowPointEntity(
                    endId = endId,
                    xNormalized = p.xNormalized,
                    yNormalized = p.yNormalized,
                    score = p.score,
                    isX = p.isX,
                    source = PointSource.MANUAL_ADDED
                )
            }
            repository.saveArrowPoints(endId, entities)
            _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
        }
    }
}
