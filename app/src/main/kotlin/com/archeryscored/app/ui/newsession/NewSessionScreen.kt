package com.archeryscored.app.ui.newsession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.archeryscored.model.TargetFaceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSessionScreen(
    onSessionCreated: (Long) -> Unit,
    viewModel: NewSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.createdSessionId) {
        uiState.createdSessionId?.let(onSessionCreated)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("New session") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = viewModel.sessionDateTime,
                onValueChange = {},
                enabled = false,
                label = { Text("Session") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.label,
                onValueChange = viewModel::onLabelChange,
                label = { Text("Label (optional)") },
                placeholder = { Text("e.g. Indoor league week 4") },
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Round", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.indoor,
                        onClick = { viewModel.onIndoorChange(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Indoor")
                    }
                    SegmentedButton(
                        selected = !uiState.indoor,
                        onClick = { viewModel.onIndoorChange(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Outdoor")
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Distance", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(STANDARD_DISTANCES_METERS) { distance ->
                        FilterChip(
                            selected = uiState.distanceMeters == distance,
                            onClick = { viewModel.onDistanceChange(distance) },
                            label = { Text("${distance}m") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Target face size", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.availableFaces, key = { it.id }) { face ->
                        FaceSizeChip(
                            face = face,
                            selected = face.id == uiState.selectedFace.id,
                            onClick = { viewModel.onFaceSelected(face) }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = viewModel::createSession,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start session")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaceSizeChip(face: TargetFaceType, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("${face.faceDiameterCm.toInt()}cm") },
        shape = RoundedCornerShape(12.dp)
    )
}
