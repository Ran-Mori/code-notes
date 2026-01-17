package com.baj.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter

/**
 * Shared component for displaying images
 */
@Composable
fun ImageViewer(
    imageUri: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    placeholder: Int = android.R.drawable.ic_menu_gallery,
    error: Int = android.R.drawable.ic_delete
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.Gray.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri.isEmpty()) {
            // Show placeholder when no image is loaded
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = placeholder),
                    contentDescription = "No image",
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No image selected",
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Load image using Coil
            AsyncImage(
                model = imageUri,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(id = placeholder),
                error = painterResource(id = error)
            )
        }
    }
}