package com.archeryscored.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscored.data.db.entity.SessionEntity
import com.archeryscored.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SessionRepository
) : ViewModel() {
    val sessions: StateFlow<List<SessionEntity>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateLabel(sessionId: Long, label: String) {
        viewModelScope.launch { repository.updateSessionLabel(sessionId, label) }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch { repository.deleteSession(sessionId) }
    }
}
