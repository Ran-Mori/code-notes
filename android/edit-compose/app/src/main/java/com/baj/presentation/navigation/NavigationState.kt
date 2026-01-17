package com.baj.presentation.navigation

/**
 * Navigation state for managing tab navigation and bottom bar visibility
 */
data class NavigationState(
    val currentTab: TabDestination = TabDestination.Adjust,
    val previousTab: TabDestination? = null,
    val isBottomBarVisible: Boolean = true
) {
    /**
     * Check if the bottom bar should be visible for a given tab
     */
    fun shouldShowBottomBar(tab: TabDestination): Boolean {
        return tab != TabDestination.Crop
    }

    /**
     * Get the tab to navigate to when back is pressed
     */
    fun getBackDestination(): TabDestination? {
        return previousTab ?: when (currentTab) {
            TabDestination.Crop -> TabDestination.Adjust
            else -> null
        }
    }
}