package com.baj.presentation.tabs.crop

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baj.presentation.components.CropControls
import com.baj.presentation.components.CropOverlay
import com.baj.presentation.components.ImageViewer

/**
 * Crop Tab Screen following MVI pattern
 */
@Composable
fun CropScreen(
    viewModel: CropViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val effect by viewModel.effect.collectAsState(initial = null)

    // Use a safe state that defaults to initial state if null
    val safeState = state ?: CropState()

    // Handle side effects
    LaunchedEffect(effect) {
        when (val currentEffect = effect) {
            is CropEffect.ShowError -> {
                // Show error snackbar or dialog
                println("Error: ${currentEffect.message}")
            }
            is CropEffect.ShowToast -> {
                // Show toast message
                println("Toast: ${currentEffect.message}")
            }
            null -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top bar with back button and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Crop",
                style = MaterialTheme.typography.h6
            )

            IconButton(onClick = { /* Handle maximize */ }) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Maximize"
                )
            }
        }

        // Image viewer with crop overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            ImageViewer(
                imageUri = safeState.imageUri,
                modifier = Modifier.fillMaxSize()
            )

            // Crop overlay
            CropOverlay(
                rotation = safeState.rotation,
                aspectRatio = safeState.cropAspectRatio,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Crop controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rotation slider
                Text(
                    text = "Rotation: ${safeState.rotation.toInt()}°",
                    style = MaterialTheme.typography.body1
                )
                Slider(
                    value = safeState.rotation,
                    onValueChange = { viewModel.processIntent(CropIntent.UpdateRotation(it)) },
                    valueRange = -45f..45f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Aspect ratio buttons
                Text(
                    text = "Aspect Ratio",
                    style = MaterialTheme.typography.body1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.processIntent(CropIntent.UpdateAspectRatio(1f)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("1:1")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.processIntent(CropIntent.UpdateAspectRatio(16f/9f)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("16:9")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.processIntent(CropIntent.UpdateAspectRatio(4f/3f)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("4:3")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.processIntent(CropIntent.UpdateAspectRatio(0f)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Free")
                    }
                }

                // Action buttons
                CropControls(
                    rotation = safeState.rotation,
                    aspectRatio = safeState.cropAspectRatio,
                    onRotationChange = { viewModel.processIntent(CropIntent.UpdateRotation(it)) },
                    onAspectRatioChange = { viewModel.processIntent(CropIntent.UpdateAspectRatio(it)) },
                    onReset = { viewModel.processIntent(CropIntent.ResetCrop) },
                    onApply = { viewModel.processIntent(CropIntent.ApplyCrop) }
                )
            }
        }
    }

    // Load initial image (mock data)
    LaunchedEffect(Unit) {
        viewModel.processIntent(CropIntent.LoadImage("https://picsum.photos/800/600"))
    }
}