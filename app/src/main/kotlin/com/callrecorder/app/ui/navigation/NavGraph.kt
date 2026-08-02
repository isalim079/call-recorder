package com.callrecorder.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Root Navigation Graph for Call Recorder.
 *
 * Implemented incrementally:
 * - Step 1:  Scaffold + visible placeholder (this file)
 * - Step 8:  Full AppScaffold + BottomNavigationBar
 * - Steps 9-14: Individual screen implementations wired in here
 *
 * All navigation is type-safe via the [Screen] sealed class.
 */
@Composable
fun CallRecorderNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) {
            // Temporary — replaced in Step 9 with HomeScreen
            ComingSoonScreen(
                title    = "Call Recorder",
                subtitle = "App is running correctly.\nFull UI coming in Step 9.",
            )
        }

        composable(Screen.Recordings.route) {
            // Placeholder — replaced in Step 10
        }

        composable(Screen.Search.route) {
            // Placeholder — replaced in Step 12
        }

        composable(Screen.Settings.route) {
            // Placeholder — replaced in Step 13
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument(Screen.Player.ARG_RECORDING_ID) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val recordingId = backStackEntry.arguments
                ?.getLong(Screen.Player.ARG_RECORDING_ID) ?: return@composable
            // Placeholder — replaced in Step 11
        }

        composable(Screen.Statistics.route) {
            // Placeholder — replaced in Step 14
        }

        composable(Screen.Storage.route) {
            // Placeholder — replaced in Step 14
        }
    }
}

// ── Temporary visible placeholder ─────────────────────────────────────────────
// This ensures the release APK shows something visible while the real
// screen implementations are built incrementally (Steps 9–14).
// Remove when real screens are wired in.

@Composable
internal fun ComingSoonScreen(title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector        = Icons.Default.Mic,
                contentDescription = null,
                modifier           = Modifier.size(72.dp),
                tint               = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text      = title,
                style     = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color     = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text      = subtitle,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
