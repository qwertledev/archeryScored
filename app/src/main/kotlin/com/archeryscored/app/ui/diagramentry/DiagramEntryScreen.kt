package com.archeryscored.app.ui.diagramentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagramEntryScreen(
    onSaved: (sessionId: Long) -> Unit,
    onBack: () -> Unit,
    viewModel: DiagramEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved(viewModel.sessionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tap arrow positions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Tap where each arrow landed. Drag a mark to adjust, long-press to remove.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            TargetDiagramInput(
                ringConfig = uiState.ringConfig,
                points = uiState.points,
                onAddPoint = viewModel::onAddPoint,
                onMovePoint = viewModel::onMovePoint,
                onDeletePoint = viewModel::onDeletePoint,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "${uiState.points.size} arrow${if (uiState.points.size == 1) "" else "s"} · ${uiState.totalScore} points",
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = viewModel::saveEnd,
                enabled = uiState.points.isNotEmpty() && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save end")
            }
        }
    }
}
