package com.archeryscored.app.ui.review

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.archeryscored.model.PointSource
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewEndScreen(
    onDone: (sessionId: Long) -> Unit,
    viewModel: ReviewEndViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onDone(viewModel.sessionId)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Review end") }) }) { padding ->
        val photoPath = uiState.photoPath
        if (uiState.isLoading || photoPath == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val bitmap = remember(photoPath) {
            BitmapFactory.decodeFile(File(context.filesDir, photoPath).absolutePath)
        }

        LaunchedEffect(bitmap) {
            bitmap?.let { viewModel.setDefaultCircleIfNeeded(it.width, it.height) }
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            InstructionBanner()

            val center = uiState.centerPx
            val radius = uiState.radiusPx
            if (bitmap != null && center != null && radius != null) {
                TargetOverlay(
                    bitmap = bitmap,
                    center = center,
                    radiusPx = radius,
                    points = uiState.points.map {
                        OverlayPoint(it.id, it.xPx, it.yPx, it.score, it.isX, confirmed = it.source != PointSource.AUTO_DETECTED)
                    },
                    onCenterChange = viewModel::onCenterChange,
                    onRadiusChange = viewModel::onRadiusChange,
                    onAddPoint = viewModel::onAddPoint,
                    onMovePoint = viewModel::onMovePoint,
                    onDeletePoint = viewModel::onDeletePoint,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            val totalScore = uiState.points.sumOf { it.score }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("End total: $totalScore", style = MaterialTheme.typography.titleMedium)
                    Text("${uiState.points.size} arrow${if (uiState.points.size == 1) "" else "s"}", style = MaterialTheme.typography.bodyMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        bitmap?.let { viewModel.resetCircle(it.width, it.height) }
                    }) {
                        Text("Reset circle")
                    }
                    Button(onClick = viewModel::save, enabled = uiState.hasCircle) {
                        Text("Save end")
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionBanner() {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            "Drag the blue handles to fit the circle to the target. Tap to add an arrow, drag a mark to adjust, long-press to remove.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
