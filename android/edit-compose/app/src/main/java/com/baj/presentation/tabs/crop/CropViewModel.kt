package com.baj.presentation.tabs.crop

import com.baj.presentation.mvi.BaseViewModel

/**
 * ViewModel for Crop Tab following MVI pattern
 */
class CropViewModel : BaseViewModel<CropState, CropIntent, CropEffect>() {

    override val initialState: CropState = CropState()

    override fun processIntent(intent: CropIntent) {
        when (intent) {
            is CropIntent.LoadImage -> loadImage(intent.imageUri)
            is CropIntent.UpdateRotation -> updateRotation(intent.degrees)
            is CropIntent.UpdateAspectRatio -> updateAspectRatio(intent.ratio)
            CropIntent.ResetCrop -> resetCrop()
            CropIntent.ApplyCrop -> applyCrop()
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
                sendEffect(CropEffect.ShowError("Failed to load image"))
            }
        }
    }

    private fun updateRotation(degrees: Float) {
        setStateValue(currentState.copy(rotation = degrees))
    }

    private fun updateAspectRatio(ratio: Float) {
        setStateValue(currentState.copy(cropAspectRatio = ratio))
    }

    private fun resetCrop() {
        setStateValue(
            currentState.copy(
                rotation = 0f,
                cropAspectRatio = 1f
            )
        )
        sendEffect(CropEffect.ShowToast("Crop settings reset"))
    }

    private fun applyCrop() {
        // In a real implementation, this would apply the crop
        sendEffect(CropEffect.ShowToast("Crop applied"))
    }
}