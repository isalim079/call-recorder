plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// ─── Read from app.properties (loaded in root build.gradle.kts) ───────────────
val appProps = rootProject.extra["appProps"] as java.util.Properties

// Helper: read a required property — fails fast with a clear message
fun prop(key: String): String =
    appProps.getProperty(key)
        ?: error("Missing required key '$key' in app.properties")

android {
    namespace   = "com.callrecorder.app"
    compileSdk  = 35

    defaultConfig {
        applicationId = prop("app.id")
        minSdk        = 26        // Android 8.0 — modern APIs, wide device coverage
        targetSdk     = 35
        versionCode   = prop("app.version.code").toInt()
        versionName   = prop("app.version.name")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // ── Inject brand resources from app.properties ────────────────────────
        // These replace the hardcoded entries that were in strings.xml / colors.xml.
        // Gradle generates them into the build/generated/res folder automatically.

        // App name (replaces <string name="app_name"> in strings.xml)
        resValue("string", "app_name", prop("app.name"))

        // Icon background + splash background (replaces ic_launcher_background in colors.xml)
        resValue("color", "ic_launcher_background", prop("app.color.dark.bg"))

        // Primary brand color — used by ic_launcher_foreground.xml via @color/app_color_primary
        resValue("color", "app_color_primary", prop("app.color.primary"))

        // Expose version name to Kotlin code via BuildConfig (in addition to auto-generated field)
        buildConfigField("String", "APP_VERSION", "\"${prop("app.version.name")}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = "password"
            keyAlias = "release"
            keyPassword = "password"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled  = false
            isDebuggable     = true
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled    = true
            isShrinkResources  = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    buildFeatures {
        compose      = true
        buildConfig  = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE.md",
                "/META-INF/LICENSE-notice.md",
            )
        }
    }

    // Lint configuration
    lint {
        abortOnError       = false
        // Disable lintVital for release — app/build lives on external media and
        // often fails with "Unable to delete ... lint-cache/migrated-jars".
        checkReleaseBuilds = false
        baseline = file("lint-baseline.xml")
    }
}


dependencies {
    // ── Core modules ──────────────────────────────────────────────────────
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:audio"))

    // ── AndroidX Core ─────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // ── Jetpack Compose ───────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // ── Navigation ────────────────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ── Hilt DI ───────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // ── WorkManager ───────────────────────────────────────────────────────
    implementation(libs.work.runtime.ktx)

    // ── Utilities ─────────────────────────────────────────────────────────
    implementation(libs.timber)
    implementation(libs.coil.compose)
    implementation(libs.biometric)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    // ── Room ──────────────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // ── Debug only ────────────────────────────────────────────────────────
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // ── Unit Tests ────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    // ── Instrumented Tests ────────────────────────────────────────────────
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
}
