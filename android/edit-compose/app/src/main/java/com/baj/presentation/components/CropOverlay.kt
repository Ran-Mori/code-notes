package com.baj.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Crop overlay component for the Crop tab
 */
@Composable
fun CropOverlay(
    rotation: Float,
    aspectRatio: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Draw crop area overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate crop area dimensions based on aspect ratio
            val cropWidth = canvasWidth * 0.8f
            val cropHeight = cropWidth / aspectRatio

            val cropX = (canvasWidth - cropWidth) / 2f
            val cropY = (canvasHeight - cropHeight) / 2f

            // Draw semi-transparent overlay outside crop area
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = Size(canvasWidth, cropY),
                topLeft = Offset(0f, 0f)
            )

            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = Size(canvasWidth, canvasHeight - cropY - cropHeight),
                topLeft = Offset(0f, cropY + cropHeight)
            )

            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = Size(cropX, cropHeight),
                topLeft = Offset(0f, cropY)
            )

            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = Size(canvasWidth - cropX - cropWidth, cropHeight),
                topLeft = Offset(cropX + cropWidth, cropY)
            )

            // Draw crop border
            drawRect(
                color = Color.White,
                topLeft = Offset(cropX, cropY),
                size = Size(cropWidth, cropHeight),
                style = Stroke(width = 2f)
            )

            // Draw corner handles
            val handleSize = 20f
            val handleThickness = 4f

            // Top-left corner
            drawLine(
                color = Color.White,
                start = Offset(cropX, cropY + handleSize),
                end = Offset(cropX, cropY),
                strokeWidth = handleThickness
            )
            drawLine(
                color = Color.White,
                start = Offset(cropX, cropY),
                end = Offset(cropX + handleSize, cropY),
                strokeWidth = handleThickness
            )

            // Top-right corner
            drawLine(
                color = Color.White,
                start = Offset(cropX + cropWidth - handleSize, cropY),
                end = Offset(cropX + cropWidth, cropY),
                strokeWidth = handleThickness
            )
            drawLine(
                color = Color.White,
                start = Offset(cropX + cropWidth, cropY),
                end = Offset(cropX + cropWidth, cropY + handleSize),
                strokeWidth = handleThickness
            )

            // Bottom-left corner
            drawLine(
                color = Color.White,
                start = Offset(cropX, cropY + cropHeight - handleSize),
                end = Offset(cropX, cropY + cropHeight),
                strokeWidth = handleThickness
            )
            drawLine(
                color = Color.White,
                start = Offset(cropX, cropY + cropHeight),
                end = Offset(cropX + handleSize, cropY + cropHeight),
                strokeWidth = handleThickness
            )

            // Bottom-right corner
            drawLine(
                color = Color.White,
                start = Offset(cropX + cropWidth - handleSize, cropY + cropHeight),
                end = Offset(cropX + cropWidth, cropY + cropHeight),
                strokeWidth = handleThickness
            )
            drawLine(
                color = Color.White,
                start = Offset(cropX + cropWidth, cropY + cropHeight - handleSize),
                end = Offset(cropX + cropWidth, cropY + cropHeight),
                strokeWidth = handleThickness
            )
        }
    }
}

/**
 * Crop controls UI (rotation and aspect ratio)
 */
@Composable
fun CropControls(
    rotation: Float,
    aspectRatio: Float,
    onRotationChange: (Float) -> Unit,
    onAspectRatioChange: (Float) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset button
        Button(
            onClick = onReset,
            modifier = Modifier.weight(1f)
        ) {
            Text("Reset")
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Rotation control
        Column(
            modifier = Modifier.weight(2f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Rotation: ${rotation.toInt()}°",
                style = MaterialTheme.typography.caption
            )
            Slider(
                value = rotation,
                onValueChange = onRotationChange,
                valueRange = -45f..45f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Apply button
        Button(
            onClick = onApply,
            modifier = Modifier.weight(1f)
        ) {
            Text("Apply")
        }
    }
}