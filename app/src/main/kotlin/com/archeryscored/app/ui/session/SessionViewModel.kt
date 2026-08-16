package com.archeryscored.app.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscored.app.util.sessionDisplayName
import com.archeryscored.data.repository.SessionRepository
import com.archeryscored.model.GroupStatsCalculator
import com.archeryscored.model.Point2D
import com.archeryscored.model.RingConfig
import com.archeryscored.model.TargetFaces
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EndSummary(val endId: Long, val endNumber: Int, val score: Int, val arrowCount: Int)

data class SessionUiState(
    val sessionName: String = "",
    val ended: Boolean = false,
    val ringConfig: RingConfig? = null,
    val allPoints: List<Point2D> = emptyList(),
    val ends: List<EndSummary> = emptyList(),
    val totalScore: Int = 0,
    val meanPoint: Point2D? = null,
    val groupRadius: Float? = null,
    val loading: Boolean = true
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository
) : ViewModel() {

    val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _finishing = MutableStateFlow(false)
    val finishing: StateFlow<Boolean> = _finishing.asStateFlow()

    val uiState: StateFlow<SessionUiState> = combine(
        repository.getSession(sessionId),
        repository.getEndsForSession(sessionId),
        repository.getPointsForSession(sessionId)
    ) { session, ends, points ->
        val ringConfig = session?.let { runCatching { TargetFaces.byId(it.targetFaceTypeId).ringConfig }.getOrNull() }
        val pointsByEnd = points.groupBy { it.endId }
        val endSummaries = ends.map { end ->
            val endPoints = pointsByEnd[end.id].orEmpty()
            EndSummary(end.id, end.endNumber, endPoints.sumOf { it.score }, endPoints.size)
        }
        // Manually-entered scores have no position to plot - only the photo-derived points count here.
        val normalizedPoints = points.mapNotNull { p ->
            val x = p.xNormalized ?: return@mapNotNull null
            val y = p.yNormalized ?: return@mapNotNull null
            Point2D(x, y)
        }
        val stats = GroupStatsCalculator.compute(normalizedPoints)
        SessionUiState(
            sessionName = session?.let { sessionDisplayName(it.createdAt, it.label) }.orEmpty(),
            ended = session?.endedAt != null,
            ringConfig = ringConfig,
            allPoints = normalizedPoints,
            ends = endSummaries,
            totalScore = points.sumOf { it.score },
            meanPoint = stats?.meanPoint,
            groupRadius = stats?.groupRadius,
            loading = session == null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionUiState())

    fun finishSession() {
        viewModelScope.launch {
            _finishing.value = true
            repository.endSession(sessionId)
            _finishing.value = false
        }
    }
}
