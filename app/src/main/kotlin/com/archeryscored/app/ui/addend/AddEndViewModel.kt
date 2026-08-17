package com.archeryscored.app.ui.addend

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscored.app.capture.EndCaptureUseCase
import com.archeryscored.app.ui.common.DiagramPoint
import com.archeryscored.data.db.entity.ArrowPointEntity
import com.archeryscored.data.repository.SessionRepository
import com.archeryscored.model.MAX_ARROWS_PER_END
import com.archeryscored.model.Point2D
import com.archeryscored.model.PointSource
import com.archeryscored.model.RingConfig
import com.archeryscored.model.ScoreCalculator
import com.archeryscored.model.TargetFaces
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import javax.inject.Inject

data class QuickScoreValue(val score: Int, val isX: Boolean) {
    val label: String get() = if (isX) "X" else if (score == 0) "M" else score.toString()
}

/** One arrow for the end being built here. [xNormalized]/[yNormalized] are only set for arrows placed by tapping the diagram. */
data class EndArrow(
    val id: Long,
    val score: Int,
    val isX: Boolean,
    val xNormalized: Float? = null,
    val yNormalized: Float? = null
) {
    val label: String get() = if (isX) "X" else if (score == 0) "M" else score.toString()
}

data class EndEntryState(
    val ringConfig: RingConfig = TargetFaces.WA_122CM.ringConfig,
    val arrows: List<EndArrow> = emptyList(),
    val isSaving: Boolean = false
) {
    val total: Int get() = arrows.sumOf { it.score }
    val canAddMore: Boolean get() = arrows.size < MAX_ARROWS_PER_END
    val diagramPoints: List<DiagramPoint>
        get() = arrows.mapNotNull { a ->
            val x = a.xNormalized ?: return@mapNotNull null
            val y = a.yNormalized ?: return@mapNotNull null
            DiagramPoint(a.id, x, y, a.score, a.isX)
        }
    /** Best score first: X (if this face has one), then max score down to 1, then Miss. */
    val palette: List<QuickScoreValue>
        get() {
            val hasX = ringConfig.boundaries.any { it.innerXRadiusRatio != null }
            return buildList {
                if (hasX) add(QuickScoreValue(ringConfig.maxScore, isX = true))
                for (score in ringConfig.maxScore downTo 1) add(QuickScoreValue(score, isX = false))
                add(QuickScoreValue(0, isX = false))
            }
        }
}

@HiltViewModel
class AddEndViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository,
    private val endCaptureUseCase: EndCaptureUseCase
) : ViewModel() {

    val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _navigateToReview = MutableStateFlow<Long?>(null)
    val navigateToReview: StateFlow<Long?> = _navigateToReview.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var nextLocalId = -1L
    private val _endEntry = MutableStateFlow(EndEntryState())
    val endEntry: StateFlow<EndEntryState> = _endEntry.asStateFlow()

    private val _endEntrySaved = MutableStateFlow(false)
    val endEntrySaved: StateFlow<Boolean> = _endEntrySaved.asStateFlow()

    init {
        viewModelScope.launch {
            val session = repository.getSession(sessionId).first()
            val face = session?.let { runCatching { TargetFaces.byId(it.targetFaceTypeId) }.getOrNull() }
            if (face != null) {
                _endEntry.value = _endEntry.value.copy(ringConfig = face.ringConfig)
            }
        }
    }

    fun onPhotoUploaded(sourceUri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            runCatching {
                val endNumber = repository.nextEndNumber(sessionId)
                val file = repository.newPhotoFile(sessionId, endNumber)
                withContext(Dispatchers.IO) { repository.importUploadedPhoto(sourceUri, file) }
                endCaptureUseCase.persistEnd(sessionId, endNumber, file)
            }.onSuccess { endId ->
                _navigateToReview.value = endId
            }.onFailure {
                _errorMessage.value = "Could not use that photo. Try a different one."
            }
            _isUploading.value = false
        }
    }

    fun addDiagramArrow(normalized: Offset) {
        val state = _endEntry.value
        if (!state.canAddMore) return
        val scored = ScoreCalculator.scoreNormalized(Point2D(normalized.x, normalized.y), state.ringConfig)
        val arrow = EndArrow(nextLocalId--, scored.score, scored.isX, normalized.x, normalized.y)
        _endEntry.value = state.copy(arrows = state.arrows + arrow)
    }

    fun moveDiagramArrow(id: Long, normalized: Offset) {
        val state = _endEntry.value
        val scored = ScoreCalculator.scoreNormalized(Point2D(normalized.x, normalized.y), state.ringConfig)
        _endEntry.value = state.copy(
            arrows = state.arrows.map {
                if (it.id == id) it.copy(score = scored.score, isX = scored.isX, xNormalized = normalized.x, yNormalized = normalized.y)
                else it
            }
        )
    }

    fun addQuickEntry(value: QuickScoreValue) {
        val state = _endEntry.value
        if (!state.canAddMore) return
        _endEntry.value = state.copy(arrows = state.arrows + EndArrow(nextLocalId--, value.score, value.isX))
    }

    fun removeArrow(id: Long) {
        _endEntry.value = _endEntry.value.copy(arrows = _endEntry.value.arrows.filterNot { it.id == id })
    }

    fun saveEndEntry() {
        val state = _endEntry.value
        if (state.arrows.isEmpty() || state.isSaving) return
        viewModelScope.launch {
            _endEntry.value = state.copy(isSaving = true)
            val endNumber = repository.nextEndNumber(sessionId)
            val endId = repository.createEnd(sessionId, endNumber, null, Clock.System.now())
            val points = state.arrows.map { arrow ->
                ArrowPointEntity(
                    endId = endId,
                    xNormalized = arrow.xNormalized,
                    yNormalized = arrow.yNormalized,
                    score = arrow.score,
                    isX = arrow.isX,
                    source = PointSource.MANUAL_ADDED
                )
            }
            repository.saveArrowPoints(endId, points)
            _endEntry.value = _endEntry.value.copy(isSaving = false)
            _endEntrySaved.value = true
        }
    }

    fun consumeNavigation() {
        _navigateToReview.value = null
    }

    fun consumeErrorMessage() {
        _errorMessage.value = null
    }
}
