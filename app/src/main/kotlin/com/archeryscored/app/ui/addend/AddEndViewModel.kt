package com.archeryscored.app.ui.addend

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscored.app.capture.EndCaptureUseCase
import com.archeryscored.data.db.entity.ArrowPointEntity
import com.archeryscored.data.repository.SessionRepository
import com.archeryscored.model.MAX_ARROWS_PER_END
import com.archeryscored.model.PointSource
import com.archeryscored.model.RingConfig
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
import javax.inject.Inject

data class QuickScoreValue(val score: Int, val isX: Boolean) {
    val label: String get() = if (isX) "X" else if (score == 0) "M" else score.toString()
}

data class QuickEntryState(
    val ringConfig: RingConfig = TargetFaces.WA_122CM.ringConfig,
    val entries: List<QuickScoreValue> = emptyList(),
    val isSaving: Boolean = false
) {
    val total: Int get() = entries.sumOf { it.score }
    val canAddMore: Boolean get() = entries.size < MAX_ARROWS_PER_END
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

    val endCount: StateFlow<Int> = repository.getEndsForSession(sessionId)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _navigateToReview = MutableStateFlow<Long?>(null)
    val navigateToReview: StateFlow<Long?> = _navigateToReview.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _quickEntry = MutableStateFlow(QuickEntryState())
    val quickEntry: StateFlow<QuickEntryState> = _quickEntry.asStateFlow()

    private val _quickEntrySaved = MutableStateFlow(false)
    val quickEntrySaved: StateFlow<Boolean> = _quickEntrySaved.asStateFlow()

    init {
        viewModelScope.launch {
            val session = repository.getSession(sessionId).first()
            val face = session?.let { runCatching { TargetFaces.byId(it.targetFaceTypeId) }.getOrNull() }
            if (face != null) {
                _quickEntry.value = _quickEntry.value.copy(ringConfig = face.ringConfig)
            }
        }
    }

    fun onPhotoUploaded(sourceUri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            runCatching {
                val endNumber = endCount.value + 1
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

    fun addQuickEntry(value: QuickScoreValue) {
        val state = _quickEntry.value
        if (!state.canAddMore) return
        _quickEntry.value = state.copy(entries = state.entries + value)
    }

    fun removeQuickEntryAt(index: Int) {
        _quickEntry.value = _quickEntry.value.copy(
            entries = _quickEntry.value.entries.filterIndexed { i, _ -> i != index }
        )
    }

    fun saveQuickEntry() {
        val state = _quickEntry.value
        if (state.entries.isEmpty() || state.isSaving) return
        viewModelScope.launch {
            _quickEntry.value = state.copy(isSaving = true)
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
            _quickEntry.value = _quickEntry.value.copy(isSaving = false)
            _quickEntrySaved.value = true
        }
    }

    fun consumeNavigation() {
        _navigateToReview.value = null
    }

    fun consumeErrorMessage() {
        _errorMessage.value = null
    }
}
