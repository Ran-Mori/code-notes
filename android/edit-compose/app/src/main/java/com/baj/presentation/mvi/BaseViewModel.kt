package com.baj.presentation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Base ViewModel for MVI architecture
 * @param S - ViewState
 * @param I - ViewIntent
 * @param E - ViewEffect
 */
abstract class BaseViewModel<S : ViewState, I : ViewIntent, E : ViewEffect> : ViewModel() {

    /**
     * Abstract property for initial state
     */
    protected abstract val initialState: S

    /**
     * Backing property for state
     */
    private val _state = MutableStateFlow<S?>(null)
    val state: StateFlow<S?> = _state.asStateFlow()

    init {
        // Initialize with the initial state
        _state.value = initialState
    }

    /**
     * Channel for side effects
     */
    private val _effect = Channel<E>(capacity = Channel.UNLIMITED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    /**
     * Current state value
     */
    protected val currentState: S
        get() = _state.value ?: initialState

    /**
     * Process user intents
     */
    abstract fun processIntent(intent: I)

    /**
     * Update state
     */
    protected fun setState(reduce: S.() -> S) {
        val current = _state.value ?: initialState
        _state.value = current.reduce()
    }

    /**
     * Update state with explicit value
     */
    protected fun setStateValue(newState: S) {
        _state.value = newState
    }

    /**
     * Send side effect
     */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    /**
     * Launch coroutine in viewModelScope
     */
    protected fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
        }
    }
}