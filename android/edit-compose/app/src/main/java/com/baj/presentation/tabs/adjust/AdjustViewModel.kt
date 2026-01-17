package com.baj.presentation.tabs.adjust

import androidx.lifecycle.viewModelScope
import com.baj.presentation.mvi.BaseViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel for Adjust Tab following MVI pattern
 */
class AdjustViewModel : BaseViewModel<AdjustState, AdjustIntent, AdjustEffect>() {

    override val initialState: AdjustState = AdjustState()

    override fun processIntent(intent: AdjustIntent) {
        when (intent) {
            is AdjustIntent.LoadImage -> loadImage(intent.imageUri)
            is AdjustIntent.UpdateExposure -> updateExposure(intent.value)
            is AdjustIntent.UpdateHighlight -> updateHighlight(intent.value)
            is AdjustIntent.UpdateShadow -> updateShadow(intent.value)
            is AdjustIntent.UpdateBrightness -> updateBrightness(intent.value)
            AdjustIntent.ResetAdjustments -> resetAdjustments()
        }
    }

    private fun loadImage(imageUri: String) {
        launch {
            setStateValue(currentState.copy(isLoading = true))
            try {
                // Simulate loading delay
                kotlinx.coroutines.delay(500)
                setStateValue(
                    currentState.copy(
                        imageUri = imageUri,
                        isLoading = false,
                        error = null
                    )
                )
            } catch (e: Exception) {
                setStateValue(
                    currentState.copy(
                        isLoading = false,
                        error = "Failed to load image"
                    )
                )
                sendEffect(AdjustEffect.ShowError("Failed to load image"))
            }
        }
    }

    private fun updateExposure(value: Float) {
        setStateValue(currentState.copy(exposure = value))
    }

    private fun updateHighlight(value: Float) {
        setStateValue(currentState.copy(highlight = value))
    }

    private fun updateShadow(value: Float) {
        setStateValue(currentState.copy(shadow = value))
    }

    private fun updateBrightness(value: Float) {
        setStateValue(currentState.copy(brightness = value))
    }

    private fun resetAdjustments() {
        setStateValue(
            currentState.copy(
                exposure = 0f,
                highlight = 0f,
                shadow = 0f,
                brightness = 0f
            )
        )
        sendEffect(AdjustEffect.ShowToast("Adjustments reset"))
    }
}