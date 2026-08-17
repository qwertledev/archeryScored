package com.archeryscored.app.ui.addend

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.archeryscored.app.ui.common.TargetDiagramInput
import com.archeryscored.model.MAX_ARROWS_PER_END

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEndScreen(
    onTakePicture: (sessionId: Long) -> Unit,
    onEndCaptured: (sessionId: Long, endId: Long) -> Unit,
    onEndEntrySaved: (sessionId: Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AddEndViewModel = hiltViewModel()
) {
    val isUploading by viewModel.isUploading.collectAsState()
    val navigateToReview by viewModel.navigateToReview.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val endEntry by viewModel.endEntry.collectAsState()
    val endEntrySaved by viewModel.endEntrySaved.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(navigateToReview) {
        navigateToReview?.let { endId ->
            onEndCaptured(viewModel.sessionId, endId)
            viewModel.consumeNavigation()
        }
    }

    LaunchedEffect(endEntrySaved) {
        if (endEntrySaved) onEndEntrySaved(viewModel.sessionId)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeErrorMessage()
        }
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::onPhotoUploaded) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add end") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to session")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isUploading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoInputButton(
                    icon = Icons.Filled.PhotoCamera,
                    label = "Camera",
                    onClick = { onTakePicture(viewModel.sessionId) },
                    modifier = Modifier.weight(1f)
                )
                PhotoInputButton(
                    icon = Icons.Filled.PhotoLibrary,
                    label = "Gallery",
                    onClick = {
                        uploadLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 2.dp))

            Text(
                if (endEntry.canAddMore) {
                    "Tap where each arrow landed, or use the values below (up to $MAX_ARROWS_PER_END per end)."
                } else {
                    "$MAX_ARROWS_PER_END arrows recorded. Remove one below to change it."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TargetDiagramInput(
                ringConfig = endEntry.ringConfig,
                points = endEntry.diagramPoints,
                onAddPoint = viewModel::addDiagramArrow,
                onMovePoint = viewModel::moveDiagramArrow,
                onDeletePoint = viewModel::removeArrow,
                modifier = Modifier.fillMaxWidth()
            )

            QuickEntryPalette(
                palette = endEntry.palette,
                enabled = endEntry.canAddMore,
                onSelect = viewModel::addQuickEntry
            )

            if (endEntry.arrows.isNotEmpty()) {
                Text(
                    "${endEntry.arrows.size} arrow${if (endEntry.arrows.size == 1) "" else "s"} · ${endEntry.total} points",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(endEntry.arrows, key = { it.id }) { arrow ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.removeArrow(arrow.id) },
                            label = { Text(arrow.label) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove") }
                        )
                    }
                }
            }

            Button(
                onClick = viewModel::saveEndEntry,
                enabled = endEntry.arrows.isNotEmpty() && !endEntry.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save end")
            }
        }
    }
}

@Composable
private fun PhotoInputButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun QuickEntryPalette(palette: List<QuickScoreValue>, enabled: Boolean, onSelect: (QuickScoreValue) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        palette.chunked(4).forEach { rowValues ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowValues.forEach { value ->
                    FilledTonalButton(
                        onClick = { onSelect(value) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(value.label, style = MaterialTheme.typography.titleMedium)
                    }
                }
                repeat(4 - rowValues.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
