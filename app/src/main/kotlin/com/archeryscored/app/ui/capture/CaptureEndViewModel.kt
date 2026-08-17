package com.archeryscored.app.ui.capture

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscored.app.capture.EndCaptureUseCase
import com.archeryscored.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CaptureEndViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository,
    private val endCaptureUseCase: EndCaptureUseCase
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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Set by prepareCapture() and reused by onPhotoSaved() so the filename and the DB row always
    // agree on the same end number, computed fresh from the DB rather than from endCount's snapshot
    // (endCount is only for the "End N" title - it's fine if it's a beat behind, but the number
    // actually saved never should be).
    private var pendingEndNumber: Int? = null

    suspend fun prepareCapture(): File {
        val endNumber = repository.nextEndNumber(sessionId)
        pendingEndNumber = endNumber
        return repository.newPhotoFile(sessionId, endNumber)
    }

    fun onPhotoSaved(file: File) {
        viewModelScope.launch {
            _isSaving.value = true
            val endNumber = pendingEndNumber ?: repository.nextEndNumber(sessionId)
            runCatching {
                endCaptureUseCase.persistEnd(sessionId, endNumber, file)
            }.onSuccess { endId ->
                _navigateToReview.value = endId
            }.onFailure {
                _errorMessage.value = "Could not save that photo. Try again."
            }
            pendingEndNumber = null
            _isSaving.value = false
        }
    }

    fun consumeErrorMessage() {
        _errorMessage.value = null
    }

    fun consumeNavigation() {
        _navigateToReview.value = null
    }
}
