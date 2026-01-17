package com.baj.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baj.presentation.navigation.BottomNavigationBar
import com.baj.presentation.navigation.NavigationState
import com.baj.presentation.navigation.TabDestination
import com.baj.presentation.tabs.adjust.AdjustScreen
import com.baj.presentation.tabs.aiErase.AiEraseScreen
import com.baj.presentation.tabs.create.CreateScreen
import com.baj.presentation.tabs.crop.CropScreen
import com.baj.presentation.tabs.filter.FilterScreen

/**
 * Main Screen with navigation setup
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var navigationState by remember {
        mutableStateOf(NavigationState())
    }

    // Update navigation state based on current destination
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            TabDestination.fromRoute(route)?.let { newTab ->
                if (newTab != navigationState.currentTab) {
                    navigationState = navigationState.copy(
                        previousTab = navigationState.currentTab,
                        currentTab = newTab,
                        isBottomBarVisible = navigationState.shouldShowBottomBar(newTab)
                    )
                }
            } ?: run {
                // Handle case where route doesn't match any tab
                navigationState = navigationState.copy(
                    isBottomBarVisible = true // Default to showing bottom bar
                )
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                isVisible = navigationState.isBottomBarVisible
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = TabDestination.Adjust.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(TabDestination.Adjust.route) {
                AdjustScreen(
                    onNavigateToCrop = {
                        navController.navigate(TabDestination.Crop.route)
                    }
                )
            }

            composable(TabDestination.Filter.route) {
                FilterScreen()
            }

            composable(TabDestination.Crop.route) {
                CropScreen(
                    onNavigateBack = {
                        // Navigate back to previous tab or Adjust as default
                        val destination = navigationState.getBackDestination() ?: TabDestination.Adjust
                        navController.navigate(destination.route) {
                            popUpTo(TabDestination.Crop.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(TabDestination.AiErase.route) {
                AiEraseScreen()
            }

            composable(TabDestination.Create.route) {
                CreateScreen()
            }
        }
    }
}