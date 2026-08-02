package com.callrecorder.app.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callrecorder.core.domain.model.CallType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recording = state.recording

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ────────────────────────────────────────────────────
            TopAppBar(
                title  = { Text("Now Playing", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector  = if (recording?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint         = if (recording?.isFavorite == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::showNotesEditor) {
                        Icon(Icons.Default.Notes, contentDescription = "Notes")
                    }
                }
            )

            if (state.isLoading || recording == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(32.dp))

                // ── Call avatar ────────────────────────────────────────────
                Box(
                    modifier         = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Default.Call,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier           = Modifier.size(56.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ── Call info ──────────────────────────────────────────────
                Text(
                    text      = recording.displayName,
                    style     = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "${if (recording.callType == CallType.INCOMING) "Incoming" else "Outgoing"} · ${recording.formattedDuration}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(48.dp))

                // ── Seek bar ───────────────────────────────────────────────
                val sliderPosition = if (state.durationMs > 0)
                    state.currentPositionMs.toFloat() / state.durationMs.toFloat()
                else 0f

                Slider(
                    value         = sliderPosition,
                    onValueChange = { fraction ->
                        viewModel.seekTo((fraction * state.durationMs).toInt())
                    },
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = SliderDefaults.colors(
                        thumbColor  = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                    enabled       = state.isReady,
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        formatMs(state.currentPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatMs(state.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(32.dp))

                // ── Playback controls ──────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::skipBackward, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Skip back 10s", modifier = Modifier.size(32.dp))
                    }

                    FilledIconButton(
                        onClick  = viewModel::togglePlayPause,
                        modifier = Modifier.size(72.dp),
                        colors   = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        enabled  = state.isReady,
                    ) {
                        Icon(
                            imageVector        = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier           = Modifier.size(36.dp),
                        )
                    }

                    IconButton(onClick = viewModel::skipForward, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Default.FastForward, contentDescription = "Skip forward 10s", modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Speed + loop controls ──────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { speed ->
                        TextButton(
                            onClick = { viewModel.setSpeed(speed) },
                        ) {
                            Text(
                                text  = "${speed}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (state.playbackSpeed == speed)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (state.playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }

                // Notes
                if (recording.notes.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text     = "Notes: ${recording.notes}",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    // ── Notes editor dialog ────────────────────────────────────────────────
    if (state.showNotesEditor) {
        var notesInput by remember { mutableStateOf(state.notesText) }
        AlertDialog(
            onDismissRequest = viewModel::dismissNotesEditor,
            title            = { Text("Recording Notes") },
            text             = {
                OutlinedTextField(
                    value         = notesInput,
                    onValueChange = { notesInput = it },
                    label         = { Text("Notes") },
                    minLines      = 3,
                    maxLines      = 6,
                )
            },
            confirmButton    = {
                TextButton(onClick = { viewModel.saveNotes(notesInput) }) { Text("Save") }
            },
            dismissButton    = {
                TextButton(onClick = viewModel::dismissNotesEditor) { Text("Cancel") }
            },
        )
    }
}

private fun formatMs(ms: Int): String {
    val totalSec = ms / 1_000
    val minutes  = totalSec / 60
    val seconds  = totalSec % 60
    return "%d:%02d".format(minutes, seconds)
}
