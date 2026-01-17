package com.baj.presentation.tabs.filter

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Filter Tab Screen - Placeholder implementation
 */
@Composable
fun FilterScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✨",
            style = MaterialTheme.typography.h3,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Filter",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Apply beautiful filters to your images",
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Placeholder for filter options
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
                    text = "Available Filters",
                    style = MaterialTheme.typography.h6
                )

                FilterOption("Vintage", "Add a retro feel")
                FilterOption("Black & White", "Classic monochrome")
                FilterOption("Vivid", "Enhanced colors")
                FilterOption("Soft", "Gentle and dreamy")
            }
        }
    }
}

@Composable
private fun FilterOption(name: String, description: String) {
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
            onClick = { /* Apply filter */ },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("Apply")
        }
    }
}