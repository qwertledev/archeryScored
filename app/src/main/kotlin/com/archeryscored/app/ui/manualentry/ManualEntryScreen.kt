package com.archeryscored.app.ui.manualentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
fun ManualEntryScreen(
    onSaved: (sessionId: Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ManualEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved(viewModel.sessionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter scores") },
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
            Text(
                "Tap a value for each arrow you shot.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.palette) { value ->
                    FilledTonalButton(
                        onClick = { viewModel.addEntry(value) },
                        modifier = Modifier.aspectRatio(1.4f)
                    ) {
                        Text(value.label, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This end: ${uiState.entries.size} arrow${if (uiState.entries.size == 1) "" else "s"} · ${uiState.total} points", style = MaterialTheme.typography.titleMedium)
                if (uiState.entries.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.entries.withIndex().toList(), key = { it.index }) { (index, entry) ->
                            InputChip(
                                selected = false,
                                onClick = { viewModel.removeEntryAt(index) },
                                label = { Text(entry.label) },
                                trailingIcon = {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove")
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = viewModel::saveEnd,
                enabled = uiState.entries.isNotEmpty() && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save end")
            }
        }
    }
}
