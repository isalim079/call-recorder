// Root build file — plugins declared here are applied in sub-modules only
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// ─── Load app.properties ──────────────────────────────────────────────────────
// Single source of truth: app name, version, ID, brand colors.
// Sub-modules access via: rootProject.extra["appProps"] as java.util.Properties
val appProps = java.util.Properties().also { props ->
    rootProject.file("app.properties").inputStream().use { props.load(it) }
}
rootProject.extra["appProps"] = appProps