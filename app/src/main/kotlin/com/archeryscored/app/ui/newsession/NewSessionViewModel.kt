package com.archeryscored.app.ui.newsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscored.app.util.formatSessionDateTime
import com.archeryscored.data.repository.SessionRepository
import com.archeryscored.model.TargetFaceType
import com.archeryscored.model.TargetFaces
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

/** The distances offered when starting a session, in meters. */
val STANDARD_DISTANCES_METERS = listOf(20, 30, 40, 70)

data class NewSessionUiState(
    val label: String = "",
    val indoor: Boolean = true,
    val distanceMeters: Int = STANDARD_DISTANCES_METERS.first(),
    val selectedFace: TargetFaceType = TargetFaces.standardRecurve.first { it.indoor },
    val createdSessionId: Long? = null
) {
    val availableFaces: List<TargetFaceType>
        get() = TargetFaces.standardRecurve.filter { it.indoor == indoor }
}

@HiltViewModel
class NewSessionViewModel @Inject constructor(
    private val repository: SessionRepository
) : ViewModel() {

    /** Computed once so the preview shown on screen exactly matches what gets saved. */
    val sessionDateTime: String = formatSessionDateTime(Clock.System.now())

    private val _uiState = MutableStateFlow(NewSessionUiState())
    val uiState: StateFlow<NewSessionUiState> = _uiState.asStateFlow()

    fun onLabelChange(value: String) {
        _uiState.value = _uiState.value.copy(label = value)
    }

    fun onIndoorChange(indoor: Boolean) {
        val state = _uiState.value
        if (state.indoor == indoor) return
        val faceForNewMode = TargetFaces.standardRecurve.first { it.indoor == indoor }
        _uiState.value = state.copy(indoor = indoor, selectedFace = faceForNewMode)
    }

    fun onDistanceChange(distanceMeters: Int) {
        _uiState.value = _uiState.value.copy(distanceMeters = distanceMeters)
    }

    fun onFaceSelected(face: TargetFaceType) {
        _uiState.value = _uiState.value.copy(selectedFace = face)
    }

    fun createSession() {
        val state = _uiState.value
        viewModelScope.launch {
            val id = repository.createSession(state.label, state.selectedFace.id, state.distanceMeters.toFloat())
            _uiState.value = _uiState.value.copy(createdSessionId = id)
        }
    }
}
