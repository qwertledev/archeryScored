package com.archeryscored.app.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    onAddEnd: (sessionId: Long) -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val finishing by viewModel.finishing.collectAsState()
    var showFinishConfirm by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text(uiState.sessionName.ifBlank { "Session" }) }) }) { padding ->
        if (uiState.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (uiState.ended) {
                AssistChip(onClick = {}, enabled = false, label = { Text("Session complete") })
            }

            Text("Total score: ${uiState.totalScore}", style = MaterialTheme.typography.titleLarge)

            if (uiState.ends.isNotEmpty()) {
                uiState.groupRadius?.let { radius ->
                    Text("Group size (max spread from mean): ${"%.1f".format(radius * 100)}% of face radius")
                }

                uiState.ringConfig?.let { ringConfig ->
                    Text("Grouping", style = MaterialTheme.typography.titleMedium)
                    GroupingChart(
                        ringConfig = ringConfig,
                        points = uiState.allPoints,
                        meanPoint = uiState.meanPoint,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Score progression", style = MaterialTheme.typography.titleMedium)
                val maxArrowsInAnEnd = uiState.ends.maxOf { it.arrowCount }.coerceAtLeast(1)
                val maxPerEnd = (uiState.ringConfig?.maxScore ?: 10) * maxArrowsInAnEnd
                ProgressionChart(ends = uiState.ends, maxPossiblePerEnd = maxPerEnd)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ends", style = MaterialTheme.typography.titleMedium)
                if (uiState.ends.isEmpty()) {
                    Text(
                        "No ends yet - add your first end below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uiState.ends.forEach { end ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("End ${end.endNumber}", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${end.score} · ${end.arrowCount} arrow${if (end.arrowCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }

            if (!uiState.ended) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onAddEnd(viewModel.sessionId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add end")
                    }
                    OutlinedButton(
                        onClick = { showFinishConfirm = true },
                        enabled = !finishing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Finish session")
                    }
                }
            }
        }
    }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text("Finish this session?") },
            text = { Text("You'll still be able to view it from Home, but you won't be able to add more ends.") },
            confirmButton = {
                Button(onClick = {
                    showFinishConfirm = false
                    viewModel.finishSession()
                }) {
                    Text("Finish")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showFinishConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
