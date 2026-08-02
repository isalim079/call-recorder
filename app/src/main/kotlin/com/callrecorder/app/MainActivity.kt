package com.callrecorder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import com.callrecorder.app.ui.AppScaffold
import com.callrecorder.app.ui.theme.AppTheme
import com.callrecorder.app.ui.theme.CallRecorderTheme
import com.callrecorder.core.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Single-activity architecture entry point.
 *
 * Responsibilities:
 * - Enable edge-to-edge rendering (status bar + nav bar overlap handled by Compose)
 * - Host the [AppScaffold] inside [CallRecorderTheme]
 * - Read the saved theme preference from [SettingsRepository] and apply it live.
 * - Hilt injection via [@AndroidEntryPoint]
 *
 * Note: Navigation, theming, and permission handling all live in Compose.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extend content behind status and navigation bars for a premium edge-to-edge feel
        enableEdgeToEdge()

        setContent {
            // Collect theme + dynamicColors from DataStore; recompose whenever they change
            val theme by settingsRepository.getTheme()
                .map { raw ->
                    when (raw) {
                        "LIGHT"  -> AppTheme.LIGHT
                        "DARK"   -> AppTheme.DARK
                        else     -> AppTheme.SYSTEM
                    }
                }
                .collectAsState(initial = AppTheme.SYSTEM)

            val dynamicColors by settingsRepository.getDynamicColors()
                .collectAsState(initial = true)

            CallRecorderTheme(appTheme = theme, dynamicColor = dynamicColors) {
                AppScaffold()
            }
        }
    }
}
