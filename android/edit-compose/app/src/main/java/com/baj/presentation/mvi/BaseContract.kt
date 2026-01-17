package com.baj.presentation.mvi

/**
 * Base contract for MVI architecture
 * Each feature will have its own contract implementing these interfaces
 */

/**
 * Base interface for all View States
 */
interface ViewState

/**
 * Base interface for all View Intents (User actions)
 */
interface ViewIntent

/**
 * Base interface for all Side Effects (One-time events)
 */
interface ViewEffect