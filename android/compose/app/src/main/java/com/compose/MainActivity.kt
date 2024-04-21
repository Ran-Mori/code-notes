package com.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    // 其实和 private val myLiveData = MutableLiveData(UIState())原理是一样的
    private val uiState = mutableStateOf(UIState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    Column {
                        Text(
                            text = "click to see loading",
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .height(80.dp)
                                .width(200.dp)
                                .border(1.dp, Color.Red)
                                .clickable {
                                    uiState.value = uiState.value.copy(
                                        error = "",
                                        isLoading = true,
                                        data = emptyList()
                                    )
                                }
                        )
                        Text(
                            text = "click to see error",
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .height(80.dp)
                                .width(200.dp)
                                .border(1.dp, Color.Red)
                                .clickable {
                                    uiState.value = uiState.value.copy(
                                        error = "error error error",
                                        isLoading = false,
                                        data = emptyList()
                                    )
                                }
                        )

                        Text(
                            text = "click to add a item",
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .height(80.dp)
                                .width(200.dp)
                                .border(1.dp, Color.Red)
                                .clickable {
                                    val oldData = uiState.value.data
                                    uiState.value = uiState.value.copy(
                                        error = "",
                                        isLoading = false,
                                        data = mutableListOf<String>().apply {
                                            addAll(oldData)
                                            add("${oldData.size + 1}")
                                        }
                                    )
                                }
                        )

                        Text(
                            text = "click to delete a item",
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .height(80.dp)
                                .width(200.dp)
                                .border(1.dp, Color.Red)
                                .clickable {
                                    val oldData = uiState.value.data.toMutableList()
                                    uiState.value = uiState.value.copy(
                                        error = "",
                                        isLoading = false,
                                        data = if (oldData.size == 0) emptyList() else oldData.apply { removeLast() }
                                    )
                                }
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            item {
                                Text(
                                    text = "______________LazyColumn start______________",
                                    modifier = Modifier.border(1.dp, Color.Red)
                                )
                            }
                            when {
                                // loading态
                                uiState.value.isLoading -> {
                                    item {
                                        Text(
                                            text = "Current now is loading",
                                            modifier = Modifier.border(1.dp, Color.Red)
                                        )
                                    }
                                }
                                // error态
                                uiState.value.error.isNotEmpty() -> {
                                    item {
                                        Text(
                                            text = "Current now is error",
                                            modifier = Modifier.border(1.dp, Color.Red)
                                        )
                                    }
                                }
                                // empty态
                                uiState.value.data.isEmpty() -> {
                                    item {
                                        Text(
                                            text = "Current now is data empty",
                                            modifier = Modifier.border(1.dp, Color.Red)
                                        )
                                    }
                                }
                                // 真正的数据
                                else -> {
                                    items(uiState.value.data) {
                                        Text(
                                            text = "this is a item, message = $it",
                                            modifier = Modifier.border(1.dp, Color.Red)
                                        )
                                    }
                                }
                            }
                            item {
                                Text(
                                    text = "______________LazyColumn end______________",
                                    modifier = Modifier.border(1.dp, Color.Red)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun SelfDefineView(message: String) {
    Row {
        Text(text = message, color = Color.Red)
        Text(text = message, color = Color.Blue)
        Text(text = message, color = Color.Green)
    }
}