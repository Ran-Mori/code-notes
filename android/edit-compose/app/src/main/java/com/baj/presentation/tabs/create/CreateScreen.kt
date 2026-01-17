package com.baj.presentation.tabs.create

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Create Tab Screen - Placeholder implementation
 */
@Composable
fun CreateScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "➕",
            style = MaterialTheme.typography.h3,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Create",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Create stunning collages and designs",
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Placeholder for creation options
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Creation Tools",
                    style = MaterialTheme.typography.h6
                )

                CreationOption("Collage", "Combine multiple photos")
                CreationOption("Meme", "Add text to images")
                CreationOption("Sticker", "Create custom stickers")
                CreationOption("Frame", "Add beautiful frames")
            }
        }
    }
}

@Composable
private fun CreationOption(name: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.body1,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }

        Button(
            onClick = { /* Start creation */ },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("Create")
        }
    }
}