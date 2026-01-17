package com.baj.presentation.tabs.crop

import com.baj.presentation.mvi.ViewEffect
import com.baj.presentation.mvi.ViewIntent
import com.baj.presentation.mvi.ViewState

/**
 * MVI Contract for Crop Tab
 */

// State
data class CropState(
    val imageUri: String = "",
    val rotation: Float = 0f,
    val cropAspectRatio: Float = 1f,
    val isLoading: Boolean = false,
    val error: String? = null
) : ViewState

// Intents (User actions)
sealed interface CropIntent : ViewIntent {
    data class LoadImage(val imageUri: String) : CropIntent
    data class UpdateRotation(val degrees: Float) : CropIntent
    data class UpdateAspectRatio(val ratio: Float) : CropIntent
    object ResetCrop : CropIntent
    object ApplyCrop : CropIntent
}

// Effects (Side effects)
sealed interface CropEffect : ViewEffect {
    data class ShowError(val message: String) : CropEffect
    data class ShowToast(val message: String) : CropEffect
}