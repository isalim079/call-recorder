package com.callrecorder.app.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.app.player.AudioPlayerEngine
import com.callrecorder.app.ui.navigation.Screen
import com.callrecorder.core.domain.model.Recording
import com.callrecorder.core.domain.usecase.GetRecordingByIdUseCase
import com.callrecorder.core.domain.usecase.ToggleFavoriteUseCase
import com.callrecorder.core.domain.usecase.UpdateNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val recording: Recording? = null,
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val isLooping: Boolean = false,
    val isReady: Boolean = false,
    val showNotesEditor: Boolean = false,
    val notesText: String = "",
    val error: String? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRecordingByIdUseCase: GetRecordingByIdUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateNotesUseCase: UpdateNotesUseCase,
    val playerEngine: AudioPlayerEngine,
) : ViewModel() {

    private val recordingId: Long = savedStateHandle[Screen.Player.ARG_RECORDING_ID] ?: 0L
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        observeRecording()
        observePlayerState()
        startPositionPoller()
    }

    private fun observeRecording() {
        viewModelScope.launch {
            getRecordingByIdUseCase(recordingId).collect { recording ->
                _uiState.update { it.copy(recording = recording, isLoading = false) }
                if (recording != null && !playerEngine.state.value.isReady) {
                    playerEngine.loadFile(recording.filePath)
                }
            }
        }
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            playerEngine.state.collect { ps ->
                _uiState.update { it.copy(
                    isPlaying         = ps.isPlaying,
                    currentPositionMs = ps.currentPositionMs,
                    durationMs        = ps.durationMs,
                    playbackSpeed     = ps.playbackSpeed,
                    isLooping         = ps.isLooping,
                    isReady           = ps.isReady,
                    error             = ps.error,
                )}
            }
        }
    }

    private fun startPositionPoller() {
        viewModelScope.launch {
            while (true) {
                delay(200)
                if (_uiState.value.isPlaying) {
                    playerEngine.updatePosition()
                }
            }
        }
    }

    fun togglePlayPause() = playerEngine.togglePlayPause()
    fun seekTo(ms: Int) = playerEngine.seekTo(ms)
    fun skipForward() = playerEngine.skipForward()
    fun skipBackward() = playerEngine.skipBackward()
    fun setSpeed(speed: Float) = playerEngine.setPlaybackSpeed(speed)
    fun setLooping(loop: Boolean) = playerEngine.setLooping(loop)

    fun toggleFavorite() {
        viewModelScope.launch { toggleFavoriteUseCase(recordingId) }
    }

    fun showNotesEditor() {
        _uiState.update { it.copy(showNotesEditor = true, notesText = it.recording?.notes ?: "") }
    }

    fun dismissNotesEditor() {
        _uiState.update { it.copy(showNotesEditor = false) }
    }

    fun saveNotes(notes: String) {
        viewModelScope.launch {
            updateNotesUseCase(recordingId, notes)
            _uiState.update { it.copy(showNotesEditor = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerEngine.release()
    }
}
