package com.archeryscored.app.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureEndScreen(
    onEndCaptured: (sessionId: Long, endId: Long) -> Unit,
    onBack: () -> Unit,
    viewModel: CaptureEndViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val endCount by viewModel.endCount.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val navigateToReview by viewModel.navigateToReview.collectAsState()

    LaunchedEffect(navigateToReview) {
        navigateToReview?.let { endId ->
            onEndCaptured(viewModel.sessionId, endId)
            viewModel.consumeNavigation()
        }
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::onPhotoUploaded) }
    val launchUpload = {
        uploadLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("End ${endCount + 1}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to session")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!hasCameraPermission) {
                PermissionRationale(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onUpload = launchUpload,
                    isSaving = isSaving
                )
            } else {
                var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
                val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder().build()
                            imageCapture = capture
                            runCatching {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )

                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator()
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(onClick = launchUpload) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = "Upload photo")
                            }
                            FloatingActionButton(onClick = {
                                val capture = imageCapture ?: return@FloatingActionButton
                                val file = viewModel.newPhotoFile()
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                                capture.takePicture(
                                    outputOptions,
                                    cameraExecutor,
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            viewModel.onPhotoSaved(file)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                        }
                                    }
                                )
                            }) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = "Capture end")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit, onUpload: () -> Unit, isSaving: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isSaving) {
            CircularProgressIndicator()
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Camera access is needed to photograph your target.")
                Button(onClick = onRequestPermission) { Text("Grant camera permission") }
                Text("or")
                FilledTonalIconButton(onClick = onUpload) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Upload photo")
                }
                Text("Upload a photo instead")
            }
        }
    }
}
