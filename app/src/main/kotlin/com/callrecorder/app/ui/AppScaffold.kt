package com.callrecorder.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.callrecorder.app.ui.home.HomeScreen
import com.callrecorder.app.ui.navigation.Screen
import com.callrecorder.app.ui.player.PlayerScreen
import com.callrecorder.app.ui.recordings.RecordingsScreen
import com.callrecorder.app.ui.search.SearchScreen
import com.callrecorder.app.ui.settings.SettingsScreen
import com.callrecorder.app.ui.statistics.StatisticsScreen
import com.callrecorder.app.ui.storage.StorageScreen

@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define which items belong to bottom navigation
    val bottomNavItems = listOf(
        Triple(Screen.Home, "Home", Icons.Default.Home),
        Triple(Screen.Recordings, "Recordings", Icons.Default.List),
        Triple(Screen.Search, "Search", Icons.Default.Search),
        Triple(Screen.Settings, "Settings", Icons.Default.Settings)
    )

    // Check if the current destination route corresponds to a bottom nav destination
    val showBottomBar = bottomNavItems.any { it.first.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                    bottomNavItems.forEach { (screen, label, icon) ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onRecordingClick = { id ->
                        navController.navigate(Screen.Player.createRoute(id))
                    },
                    onSeeAllRecordings = {
                        navController.navigate(Screen.Recordings.route)
                    },
                    onStorageClick = {
                        navController.navigate(Screen.Storage.route)
                    },
                    onStatisticsClick = {
                        navController.navigate(Screen.Statistics.route)
                    }
                )
            }

            composable(Screen.Recordings.route) {
                RecordingsScreen(
                    onRecordingClick = { id ->
                        navController.navigate(Screen.Player.createRoute(id))
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onRecordingClick = { id ->
                        navController.navigate(Screen.Player.createRoute(id))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument(Screen.Player.ARG_RECORDING_ID) {
                        type = NavType.LongType
                    }
                )
            ) {
                PlayerScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Statistics.route) {
                StatisticsScreen()
            }

            composable(Screen.Storage.route) {
                StorageScreen(
                    onBack = { navController.popBackStack() },
                    onRecordingClick = { id ->
                        navController.navigate(Screen.Player.createRoute(id))
                    }
                )
            }
        }
    }
}
