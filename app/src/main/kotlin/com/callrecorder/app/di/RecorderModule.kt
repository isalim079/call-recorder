package com.callrecorder.app.di

import com.callrecorder.app.recorder.AudioRecorderEngine
import com.callrecorder.app.recorder.MediaRecorderEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped

/**
 * Hilt module providing the audio recorder engine.
 *
 * Scoped to [ServiceComponent] — each [CallRecorderService] instance gets
 * its own [MediaRecorderEngine]. This prevents the engine from being shared
 * between service instances (important for correct MediaRecorder state).
 */
@Module
@InstallIn(ServiceComponent::class)
abstract class RecorderModule {

    @Binds
    @ServiceScoped
    abstract fun bindAudioRecorderEngine(
        impl: MediaRecorderEngine,
    ): AudioRecorderEngine
}
