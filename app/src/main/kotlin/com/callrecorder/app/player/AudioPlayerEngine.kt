package com.callrecorder.app.player

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
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
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnPreparedListener { mp ->
                    _state.value = _state.value.copy(
                        durationMs = mp.duration,
                        isReady    = true,
                    )
                    if (autoPlay) {
                        mp.start()
                        _state.value = _state.value.copy(isPlaying = true)
                    }
                }
                setOnCompletionListener {
                    _state.value = _state.value.copy(
                        isPlaying        = false,
                        currentPositionMs = 0,
                    )
                }
                setOnErrorListener { _, what, extra ->
                    Timber.e("MediaPlayer error: what=$what extra=$extra")
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        error     = "Playback error ($what)"
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
                mp.start()
                _state.value = _state.value.copy(isPlaying = true)
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
        mediaPlayer?.seekTo(positionMs)
        _state.value = _state.value.copy(currentPositionMs = positionMs)
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
        mediaPlayer?.playbackParams = mediaPlayer!!.playbackParams.setSpeed(speed)
        _state.value = _state.value.copy(playbackSpeed = speed)
    }

    fun setLooping(looping: Boolean) {
        mediaPlayer?.isLooping = looping
        _state.value = _state.value.copy(isLooping = looping)
    }

    fun getCurrentPosition(): Int = try {
        mediaPlayer?.currentPosition ?: 0
    } catch (e: Exception) { 0 }

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
