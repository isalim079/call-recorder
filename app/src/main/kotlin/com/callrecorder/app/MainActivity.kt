package com.callrecorder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.callrecorder.app.ui.AppScaffold
import com.callrecorder.app.ui.theme.CallRecorderTheme

/**
 * Single-activity architecture entry point.
 *
 * Responsibilities:
 * - Enable edge-to-edge rendering (status bar + nav bar overlap handled by Compose)
 * - Host the [AppScaffold] inside [CallRecorderTheme]
 * - Hilt injection via [@AndroidEntryPoint]
 *
 * Note: Navigation, theming, and permission handling all live in Compose.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extend content behind status and navigation bars for a premium edge-to-edge feel
        enableEdgeToEdge()

        setContent {
            CallRecorderTheme {
                AppScaffold()
            }
        }
    }
}
