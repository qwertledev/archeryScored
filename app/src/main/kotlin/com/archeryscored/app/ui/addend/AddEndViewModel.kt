package com.archeryscored.app.ui.addend

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscored.app.capture.EndCaptureUseCase
import com.archeryscored.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

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

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _navigateToReview = MutableStateFlow<Long?>(null)
    val navigateToReview: StateFlow<Long?> = _navigateToReview.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun onPhotoUploaded(sourceUri: Uri) {
        viewModelScope.launch {
            _isSaving.value = true
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
            _isSaving.value = false
        }
    }

    fun consumeNavigation() {
        _navigateToReview.value = null
    }

    fun consumeErrorMessage() {
        _errorMessage.value = null
    }
}
