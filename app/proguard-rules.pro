# =============================================================================
# ProGuard / R8 rules — Call Recorder
# =============================================================================
# Strategy: keep all entry points that R8 cannot infer statically.
# Hilt, Room, Compose, Coroutines all require specific rules in release builds.
# =============================================================================

# ── General Kotlin / JVM attributes ─────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions

# ── Application entry points ─────────────────────────────────────────────────
# Keep the Application class and all Activity / Service / BroadcastReceiver
# subclasses that are declared in AndroidManifest.xml.
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ── Hilt (Dagger) ────────────────────────────────────────────────────────────
# R8 must not touch Hilt-generated _HiltComponents and _MembersInjector classes.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @javax.inject.Singleton class * { *; }

# Keep all Hilt-generated classes (suffixed with _HiltModules, _Factory, etc.)
-keep class **_Hilt* { *; }
-keep class **Hilt_* { *; }
-keep class *_Factory { *; }
-keep class *_MembersInjector { *; }

-dontwarn dagger.**
-dontwarn hilt_aggregated_deps.**

# ── Room ─────────────────────────────────────────────────────────────────────
# Room entities and DAOs use reflection at runtime.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.room.** { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# ── Jetpack Compose ──────────────────────────────────────────────────────────
# Compose uses @Composable annotations and reflection for previews/tooling.
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**

# ── Kotlin Coroutines ────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── Kotlin Serialization (if used in future) ─────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ── DataStore (Preferences) ─────────────────────────────────────────────────
# DataStore uses reflection to read/write preferences keys.
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn androidx.datastore.**

# ── Navigation Compose ───────────────────────────────────────────────────────
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ── ViewModel (Lifecycle) ────────────────────────────────────────────────────
# ViewModels are instantiated by ViewModelProvider factories.
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ── WorkManager ──────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# ── Coil (Image loading) ─────────────────────────────────────────────────────
-dontwarn coil.**
-keep class coil.** { *; }

# ── Timber ───────────────────────────────────────────────────────────────────
-dontwarn org.jetbrains.annotations.**
-keep class timber.log.Timber { *; }

# ── Biometric ────────────────────────────────────────────────────────────────
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# ── Kotlin Reflect ───────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlin.reflect.jvm.internal.**

# ── Enum classes ─────────────────────────────────────────────────────────────
# Our domain enums are referenced by name (stored as Strings in Room).
# R8 must not rename them.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public final java.lang.String name();
    public final int ordinal();
}
-keep enum com.callrecorder.** { *; }

# ── Data classes (domain models) ─────────────────────────────────────────────
# Kotlin data classes used in Flow/StateFlow must keep their field names.
-keep class com.callrecorder.core.domain.model.** { *; }
-keep class com.callrecorder.core.database.entity.** { *; }
-keep class com.callrecorder.core.database.dao.** { *; }

# ── App-level keep (our own classes) ─────────────────────────────────────────
-keep class com.callrecorder.app.** { *; }
-keep class com.callrecorder.core.** { *; }
