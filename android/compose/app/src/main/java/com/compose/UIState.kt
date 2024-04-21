package com.compose

data class UIState(
    var data: List<String> = emptyList(),
    var isLoading: Boolean = false,
    var error: String = ""
)