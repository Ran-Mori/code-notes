package com.baj.presentation.tabs.adjust

import com.baj.presentation.mvi.ViewEffect
import com.baj.presentation.mvi.ViewIntent
import com.baj.presentation.mvi.ViewState

/**
 * MVI Contract for Adjust Tab
 */

// State
data class AdjustState(
    val imageUri: String = "",
    val exposure: Float = 0f,
    val highlight: Float = 0f,
    val shadow: Float = 0f,
    val brightness: Float = 0f,
    val isLoading: Boolean = false,
    val error: String? = null
) : ViewState

// Intents (User actions)
sealed interface AdjustIntent : ViewIntent {
    data class LoadImage(val imageUri: String) : AdjustIntent
    data class UpdateExposure(val value: Float) : AdjustIntent
    data class UpdateHighlight(val value: Float) : AdjustIntent
    data class UpdateShadow(val value: Float) : AdjustIntent
    data class UpdateBrightness(val value: Float) : AdjustIntent
    object ResetAdjustments : AdjustIntent
}

// Effects (Side effects)
sealed interface AdjustEffect : ViewEffect {
    data class ShowError(val message: String) : AdjustEffect
    data class ShowToast(val message: String) : AdjustEffect
}