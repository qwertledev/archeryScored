package com.archeryscored.app.ui.manualentry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscored.data.db.entity.ArrowPointEntity
import com.archeryscored.data.repository.SessionRepository
import com.archeryscored.model.PointSource
import com.archeryscored.model.RingConfig
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

data class ManualScoreValue(val score: Int, val isX: Boolean) {
    val label: String get() = if (isX) "X" else if (score == 0) "M" else score.toString()
}

data class ManualEntryUiState(
    val ringConfig: RingConfig = TargetFaces.WA_122CM.ringConfig,
    val entries: List<ManualScoreValue> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false
) {
    val total: Int get() = entries.sumOf { it.score }
    /** Palette offered, best score first: X (if this face has one), then max score down to 1, then Miss. */
    val palette: List<ManualScoreValue>
        get() {
            val hasX = ringConfig.boundaries.any { it.innerXRadiusRatio != null }
            val values = buildList {
                if (hasX) add(ManualScoreValue(ringConfig.maxScore, isX = true))
                for (score in ringConfig.maxScore downTo 1) add(ManualScoreValue(score, isX = false))
                add(ManualScoreValue(0, isX = false))
            }
            return values
        }
}

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository
) : ViewModel() {

    val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val endCount: StateFlow<Int> = repository.getEndsForSession(sessionId)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _uiState = MutableStateFlow(ManualEntryUiState())
    val uiState: StateFlow<ManualEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = repository.getSession(sessionId).first()
            val face = session?.let { runCatching { TargetFaces.byId(it.targetFaceTypeId) }.getOrNull() }
            if (face != null) {
                _uiState.value = _uiState.value.copy(ringConfig = face.ringConfig)
            }
        }
    }

    fun addEntry(value: ManualScoreValue) {
        _uiState.value = _uiState.value.copy(entries = _uiState.value.entries + value)
    }

    fun removeEntryAt(index: Int) {
        _uiState.value = _uiState.value.copy(
            entries = _uiState.value.entries.filterIndexed { i, _ -> i != index }
        )
    }

    fun saveEnd() {
        val state = _uiState.value
        if (state.entries.isEmpty() || state.isSaving) return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            val endNumber = endCount.value + 1
            val endId = repository.createEnd(sessionId, endNumber, null, Clock.System.now())
            val points = state.entries.map { entry ->
                ArrowPointEntity(
                    endId = endId,
                    xNormalized = null,
                    yNormalized = null,
                    score = entry.score,
                    isX = entry.isX,
                    source = PointSource.MANUAL_ADDED
                )
            }
            repository.saveArrowPoints(endId, points)
            _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
        }
    }
}
