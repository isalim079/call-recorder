package com.callrecorder.app.ui.navigation

/**
 * Sealed class representing all navigation destinations in the app.
 *
 * Each object holds the route string used by Navigation Compose.
 * Arguments are passed as path or query parameters.
 *
 * Navigation graph implementation is in [CallRecorderNavGraph].
 */
sealed class Screen(val route: String) {

    // ── Bottom navigation destinations ─────────────────────────────────────
    data object Home : Screen("home")
    data object Recordings : Screen("recordings")
    data object Search : Screen("search")
    data object Settings : Screen("settings")

    // ── Nested / detail destinations ───────────────────────────────────────
    data object Player : Screen("player/{recordingId}") {
        const val ARG_RECORDING_ID = "recordingId"
        fun createRoute(recordingId: Long) = "player/$recordingId"
    }

    data object Statistics : Screen("statistics")
    data object Storage : Screen("storage")
    data object PinSetup : Screen("pin_setup")
    data object AppLock : Screen("app_lock")

    // ── Computed: is this a bottom-nav destination? ────────────────────────
    val isBottomNavItem: Boolean
        get() = this in listOf(Home, Recordings, Search, Settings)
}
