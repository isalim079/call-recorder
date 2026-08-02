package com.callrecorder.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callrecorder.app.ui.components.EmptyStateView
import com.callrecorder.app.ui.components.RecordingListItem

@Composable
fun SearchScreen(
    onRecordingClick: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(16.dp))
        // ── Search input ───────────────────────────────────────────────────
        TextField(
            value         = state.query,
            onValueChange = viewModel::setQuery,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder   = { Text("Search by name, number, notes…") },
            leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon  = {
                if (state.query.isNotBlank()) {
                    IconButton(onClick = viewModel::clearQuery) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine    = true,
            shape         = MaterialTheme.shapes.extraLarge,
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )

        Spacer(Modifier.height(12.dp))

        // ── Empty / results ────────────────────────────────────────────────
        when {
            state.query.isBlank() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Default.Search,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier           = Modifier.padding(16.dp),
                    )
                    Text(
                        "Start typing to search",
                        style     = MaterialTheme.typography.bodyLarge,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            state.isSearching -> {
                Column(
                    modifier            = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.results.isEmpty() -> {
                EmptyStateView(
                    icon     = Icons.Default.Search,
                    title    = "No results",
                    subtitle = "No recordings match \"${state.query}\"",
                )
            }

            else -> {
                LazyColumn(
                    contentPadding      = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.results, key = { it.id }) { recording ->
                        RecordingListItem(
                            recording        = recording,
                            onClick          = { onRecordingClick(recording.id) },
                            onFavoriteToggle = { viewModel.toggleFavorite(recording.id) },
                            onDelete         = { viewModel.deleteRecording(recording.id) },
                            onRename         = { viewModel.showRenameDialog(recording.id, recording.displayName) },
                            onShare          = { },
                            modifier         = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }

    // ── Rename dialog ──────────────────────────────────────────────────────
    if (state.showRenameDialog) {
        var nameText by remember(state.renameInitialName) { mutableStateOf(state.renameInitialName) }
        AlertDialog(
            onDismissRequest = viewModel::dismissRenameDialog,
            title            = { Text("Rename Recording") },
            text             = {
                OutlinedTextField(
                    value         = nameText,
                    onValueChange = { nameText = it },
                    label         = { Text("Name") },
                    singleLine    = true,
                )
            },
            confirmButton    = {
                TextButton(
                    onClick = { viewModel.renameRecording(nameText) },
                    enabled = nameText.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton    = {
                TextButton(onClick = viewModel::dismissRenameDialog) { Text("Cancel") }
            },
        )
    }
}
