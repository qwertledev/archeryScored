package com.archeryscored.app.ui.addend

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.archeryscored.model.MAX_ARROWS_PER_END

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEndScreen(
    onTakePicture: (sessionId: Long) -> Unit,
    onEnterViaDiagram: (sessionId: Long) -> Unit,
    onEndCaptured: (sessionId: Long, endId: Long) -> Unit,
    onQuickEntrySaved: (sessionId: Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AddEndViewModel = hiltViewModel()
) {
    val isUploading by viewModel.isUploading.collectAsState()
    val navigateToReview by viewModel.navigateToReview.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val quickEntry by viewModel.quickEntry.collectAsState()
    val quickEntrySaved by viewModel.quickEntrySaved.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(navigateToReview) {
        navigateToReview?.let { endId ->
            onEndCaptured(viewModel.sessionId, endId)
            viewModel.consumeNavigation()
        }
    }

    LaunchedEffect(quickEntrySaved) {
        if (quickEntrySaved) onQuickEntrySaved(viewModel.sessionId)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Photograph or mark the target",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ChooserCard(
                icon = Icons.Filled.PhotoCamera,
                title = "Take a picture",
                subtitle = "Photograph the target with the camera",
                onClick = { onTakePicture(viewModel.sessionId) }
            )
            ChooserCard(
                icon = Icons.Filled.PhotoLibrary,
                title = "Use a picture",
                subtitle = "Pick an existing photo from your device",
                onClick = {
                    uploadLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
            ChooserCard(
                icon = Icons.Filled.GpsFixed,
                title = "Tap on a target",
                subtitle = "Mark where each arrow landed on a blank target face - no photo",
                onClick = { onEnterViaDiagram(viewModel.sessionId) }
            )

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            Text("Or enter scores directly", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (quickEntry.canAddMore) {
                    "Tap a value for each arrow (up to $MAX_ARROWS_PER_END per end)."
                } else {
                    "$MAX_ARROWS_PER_END arrows recorded. Remove one below to change it."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            QuickEntryPalette(
                palette = quickEntry.palette,
                enabled = quickEntry.canAddMore,
                onSelect = viewModel::addQuickEntry
            )

            if (quickEntry.entries.isNotEmpty()) {
                Text(
                    "${quickEntry.entries.size} arrow${if (quickEntry.entries.size == 1) "" else "s"} · ${quickEntry.total} points",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickEntry.entries.withIndex().toList(), key = { it.index }) { (index, entry) ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.removeQuickEntryAt(index) },
                            label = { Text(entry.label) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove") }
                        )
                    }
                }
            }

            Button(
                onClick = viewModel::saveQuickEntry,
                enabled = quickEntry.entries.isNotEmpty() && !quickEntry.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save end")
            }
        }
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

@Composable
private fun ChooserCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
