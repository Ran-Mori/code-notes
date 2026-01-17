package com.baj.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Custom slider component for image adjustments
 */
@Composable
fun AdjustmentSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = -100f..100f,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${value.toInt()}",
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.End,
                modifier = Modifier.width(40.dp)
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.primary,
                activeTrackColor = MaterialTheme.colors.primary,
                inactiveTrackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
            )
        )
    }
}

/**
 * Row of adjustment controls for the Adjust tab
 */
@Composable
fun AdjustmentControlsRow(
    exposure: Float,
    highlight: Float,
    shadow: Float,
    brightness: Float,
    onExposureChange: (Float) -> Unit,
    onHighlightChange: (Float) -> Unit,
    onShadowChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Exposure control
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "E",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary
            )
            Text(
                text = "Exposure",
                style = MaterialTheme.typography.caption
            )
            Text(
                text = "${exposure.toInt()}",
                style = MaterialTheme.typography.body1
            )
        }

        // Highlight control
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "H",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary
            )
            Text(
                text = "Highlight",
                style = MaterialTheme.typography.caption
            )
            Text(
                text = "${highlight.toInt()}",
                style = MaterialTheme.typography.body1
            )
        }

        // Shadow control
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "S",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary
            )
            Text(
                text = "Shadow",
                style = MaterialTheme.typography.caption
            )
            Text(
                text = "${shadow.toInt()}",
                style = MaterialTheme.typography.body1
            )
        }

        // Brightness control
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "B",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary
            )
            Text(
                text = "Bright",
                style = MaterialTheme.typography.caption
            )
            Text(
                text = "${brightness.toInt()}",
                style = MaterialTheme.typography.body1
            )
        }
    }
}