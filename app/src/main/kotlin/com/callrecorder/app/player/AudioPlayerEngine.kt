package com.callrecorder.app.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Wraps Android [MediaPlayer] in a coroutine-friendly API.
 *
 * Manages the full MediaPlayer lifecycle:
 * prepare → start → pause → seekTo → stop → release
 *
 * This is a stateful object. Create one per player screen session.
 * Call [release] when done.
 */
class AudioPlayerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class PlayerState(
        val isPlaying: Boolean = false,
        val currentPositionMs: Int = 0,
        val durationMs: Int = 0,
        val playbackSpeed: Float = 1.0f,
        val isLooping: Boolean = false,
        val isReady: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    /**
     * Load an audio file from [filePath] and prepare it for playback.
     *
     * @param filePath Absolute path to the .m4a file.
     * @param autoPlay Start playback immediately after preparation.
     */
    fun loadFile(filePath: String, autoPlay: Boolean = false) {
        releasePlayer()
        try {
            val file = File(filePath)
            if (!file.exists() || file.length() < 64) {
                Timber.e("Audio file missing or too small: $filePath size=${file.length()}")
                _state.value = _state.value.copy(error = "Recording file missing or empty")
                return
            }

            mediaPlayer = MediaPlayer().apply {
                // Route to media stream (speaker/headphones), not in-call / silent routes
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setVolume(1f, 1f)
                setDataSource(filePath)
                setOnPreparedListener { mp ->
                    mp.setVolume(1f, 1f)
                    _state.value = _state.value.copy(
                        durationMs = mp.duration.coerceAtLeast(0),
                        isReady    = true,
                        error      = null,
                    )
                    if (autoPlay) {
                        mp.start()
                        _state.value = _state.value.copy(isPlaying = true)
                    }
                }
                setOnCompletionListener {
                    _state.value = _state.value.copy(
                        isPlaying         = false,
                        currentPositionMs = 0,
                    )
                }
                setOnErrorListener { _, what, extra ->
                    Timber.e("MediaPlayer error: what=$what extra=$extra path=$filePath")
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isReady   = false,
                        error     = "Playback error ($what)",
                    )
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load audio file: $filePath")
            _state.value = _state.value.copy(error = e.message)
        }
    }

    fun play() {
        mediaPlayer?.let { mp ->
            if (!mp.isPlaying) {
                try {
                    mp.setVolume(1f, 1f)
                    mp.start()
                    _state.value = _state.value.copy(isPlaying = true)
                } catch (e: Exception) {
                    Timber.e(e, "play() failed")
                    _state.value = _state.value.copy(error = e.message)
                }
            }
        }
    }

    fun pause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _state.value = _state.value.copy(isPlaying = false)
            }
        }
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else play()
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs.coerceAtLeast(0))
            _state.value = _state.value.copy(currentPositionMs = positionMs.coerceAtLeast(0))
        } catch (e: Exception) {
            Timber.w(e, "seekTo failed")
        }
    }

    fun skipForward(ms: Int = 10_000) {
        val newPos = (getCurrentPosition() + ms).coerceAtMost(_state.value.durationMs)
        seekTo(newPos)
    }

    fun skipBackward(ms: Int = 10_000) {
        val newPos = (getCurrentPosition() - ms).coerceAtLeast(0)
        seekTo(newPos)
    }

    fun setPlaybackSpeed(speed: Float) {
        try {
            val mp = mediaPlayer ?: return
            mp.playbackParams = mp.playbackParams.setSpeed(speed)
            _state.value = _state.value.copy(playbackSpeed = speed)
        } catch (e: Exception) {
            Timber.w(e, "setPlaybackSpeed failed")
        }
    }

    fun setLooping(looping: Boolean) {
        mediaPlayer?.isLooping = looping
        _state.value = _state.value.copy(isLooping = looping)
    }

    fun getCurrentPosition(): Int = try {
        mediaPlayer?.currentPosition ?: 0
    } catch (e: Exception) {
        0
    }

    fun updatePosition() {
        val pos = getCurrentPosition()
        if (_state.value.currentPositionMs != pos) {
            _state.value = _state.value.copy(currentPositionMs = pos)
        }
    }

    fun release() = releasePlayer()

    private fun releasePlayer() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error releasing MediaPlayer")
        } finally {
            mediaPlayer = null
            _state.value = PlayerState()
        }
    }
}
