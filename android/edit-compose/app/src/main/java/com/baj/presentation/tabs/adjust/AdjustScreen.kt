package com.baj.presentation.tabs.adjust

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baj.presentation.components.AdjustmentControlsRow
import com.baj.presentation.components.ImageViewer

/**
 * Adjust Tab Screen following MVI pattern
 */
@Composable
fun AdjustScreen(
    viewModel: AdjustViewModel = viewModel(),
    onNavigateToCrop: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val effect by viewModel.effect.collectAsState(initial = null)

    // Use a safe state that defaults to initial state if null
    val safeState = state ?: AdjustState()

    // Handle side effects
    LaunchedEffect(effect) {
        when (val currentEffect = effect) {
            is AdjustEffect.ShowError -> {
                // Show error snackbar or dialog
                println("Error: ${currentEffect.message}")
            }
            is AdjustEffect.ShowToast -> {
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
        // Image viewer
        ImageViewer(
            imageUri = safeState.imageUri,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Top bar with Cancel and Done buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { /* Handle cancel */ }) {
                Text("Cancel")
            }

            TextButton(onClick = { /* Handle done */ }) {
                Text("Done")
            }
        }

        // Adjustment controls
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
                // Quick adjustment buttons row (matching ASCII design)
                AdjustmentControlsRow(
                    exposure = safeState.exposure,
                    highlight = safeState.highlight,
                    shadow = safeState.shadow,
                    brightness = safeState.brightness,
                    onExposureChange = { viewModel.processIntent(AdjustIntent.UpdateExposure(it)) },
                    onHighlightChange = { viewModel.processIntent(AdjustIntent.UpdateHighlight(it)) },
                    onShadowChange = { viewModel.processIntent(AdjustIntent.UpdateShadow(it)) },
                    onBrightnessChange = { viewModel.processIntent(AdjustIntent.UpdateBrightness(it)) }
                )

                Divider()

                // Individual adjustment sliders
                Text(
                    text = "Fine Adjustments",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Exposure slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Exposure", modifier = Modifier.weight(1f), style = MaterialTheme.typography.body1)
                    Slider(
                        value = safeState.exposure,
                        onValueChange = { viewModel.processIntent(AdjustIntent.UpdateExposure(it)) },
                        valueRange = -100f..100f,
                        modifier = Modifier.weight(2f)
                    )
                    Text("${safeState.exposure.toInt()}", modifier = Modifier.width(40.dp))
                }

                // Highlight slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Highlight", modifier = Modifier.weight(1f), style = MaterialTheme.typography.body1)
                    Slider(
                        value = safeState.highlight,
                        onValueChange = { viewModel.processIntent(AdjustIntent.UpdateHighlight(it)) },
                        valueRange = -100f..100f,
                        modifier = Modifier.weight(2f)
                    )
                    Text("${safeState.highlight.toInt()}", modifier = Modifier.width(40.dp))
                }

                // Shadow slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Shadow", modifier = Modifier.weight(1f), style = MaterialTheme.typography.body1)
                    Slider(
                        value = safeState.shadow,
                        onValueChange = { viewModel.processIntent(AdjustIntent.UpdateShadow(it)) },
                        valueRange = -100f..100f,
                        modifier = Modifier.weight(2f)
                    )
                    Text("${safeState.shadow.toInt()}", modifier = Modifier.width(40.dp))
                }

                // Brightness slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Brightness", modifier = Modifier.weight(1f), style = MaterialTheme.typography.body1)
                    Slider(
                        value = safeState.brightness,
                        onValueChange = { viewModel.processIntent(AdjustIntent.UpdateBrightness(it)) },
                        valueRange = -100f..100f,
                        modifier = Modifier.weight(2f)
                    )
                    Text("${safeState.brightness.toInt()}", modifier = Modifier.width(40.dp))
                }

                // Reset button
                Button(
                    onClick = { viewModel.processIntent(AdjustIntent.ResetAdjustments) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Reset All")
                }
            }
        }
    }

    // Load initial image (mock data)
    LaunchedEffect(Unit) {
        viewModel.processIntent(AdjustIntent.LoadImage("https://picsum.photos/800/800"))
    }
}