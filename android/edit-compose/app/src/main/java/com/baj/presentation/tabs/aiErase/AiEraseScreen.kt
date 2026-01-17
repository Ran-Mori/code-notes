package com.baj.presentation.tabs.aiErase

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * AI Erase Tab Screen - Placeholder implementation
 */
@Composable
fun AiEraseScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🧹",
            style = MaterialTheme.typography.h3,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "AI Erase",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Remove unwanted objects with AI",
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Placeholder for AI erase functionality
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI Magic Wand",
                    style = MaterialTheme.typography.h6
                )

                Text(
                    text = "Simply brush over the objects you want to remove",
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = { /* Start AI erase */ },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Start Erasing")
                }
            }
        }
    }
}