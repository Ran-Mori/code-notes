package com.baj.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Bottom navigation bar for the 5 tabs
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    BottomNavigation(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: ""

        TabDestination.allTabs.forEach { tab ->
            val isSelected = currentRoute == tab.route

            BottomNavigationItem(
                icon = {
                    Text(
                        text = tab.icon,
                        style = MaterialTheme.typography.body1
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.caption
                    )
                },
                selected = isSelected,
                onClick = {
                    // Only navigate if not already on this tab
                    if (!isSelected) {
                        navController.navigate(tab.route) {
                            // Pop up to the start destination of the graph to avoid building up a large stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                },
                selectedContentColor = MaterialTheme.colors.primary,
                unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                alwaysShowLabel = true
            )
        }
    }
}