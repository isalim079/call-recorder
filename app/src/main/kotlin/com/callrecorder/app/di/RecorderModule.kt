package com.callrecorder.app.di

import com.callrecorder.app.recorder.AudioRecorderEngine
import com.callrecorder.app.recorder.HdRecorderEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped

/**
 * Hilt module providing the audio recorder engine.
 *
 * Primary: [HdRecorderEngine] (denoise + skip-ring via [com.callrecorder.core.audio]).
 */
@Module
@InstallIn(ServiceComponent::class)
abstract class RecorderModule {

    @Binds
    @ServiceScoped
    abstract fun bindAudioRecorderEngine(
        impl: HdRecorderEngine,
    ): AudioRecorderEngine
}
