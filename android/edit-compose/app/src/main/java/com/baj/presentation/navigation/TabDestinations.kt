package com.baj.presentation.navigation

/**
 * Navigation destinations for the 5 tabs
 */
sealed class TabDestination(val route: String, val title: String, val icon: String) {
    object Adjust : TabDestination("adjust", "Adjust", "🎨")
    object Filter : TabDestination("filter", "Filter", "✨")
    object Crop : TabDestination("crop", "Crop", "✂️")
    object AiErase : TabDestination("ai_erase", "AI Erase", "🧹")
    object Create : TabDestination("create", "Create", "➕")

    companion object {
        val allTabs
            get() = listOf(Adjust, Filter, Crop, AiErase, Create)

        fun fromRoute(route: String?): TabDestination? {
            return allTabs.find { it.route == route }
        }
    }
}