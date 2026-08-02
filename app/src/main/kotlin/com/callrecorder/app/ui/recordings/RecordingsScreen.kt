package com.callrecorder.app.ui.recordings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callrecorder.app.ui.components.EmptyStateView
import com.callrecorder.app.ui.components.RecordingListItem
import com.callrecorder.core.domain.model.CallType
import com.callrecorder.core.domain.model.RecordingSortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    onRecordingClick: (Long) -> Unit,
    viewModel: RecordingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Search bar ─────────────────────────────────────────────────────
        TextField(
            value         = state.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder   = { Text("Search recordings…") },
            leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon  = {
                if (state.searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
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

        // ── Filter chips ───────────────────────────────────────────────────
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.filterCallType == null && !state.filterFavoritesOnly,
                onClick  = { viewModel.setFilterCallType(null); viewModel.setFavoritesOnly(false) },
                label    = { Text("All") },
            )
            FilterChip(
                selected = state.filterCallType == CallType.INCOMING,
                onClick  = { viewModel.setFilterCallType(CallType.INCOMING) },
                label    = { Text("Incoming") },
            )
            FilterChip(
                selected = state.filterCallType == CallType.OUTGOING,
                onClick  = { viewModel.setFilterCallType(CallType.OUTGOING) },
                label    = { Text("Outgoing") },
            )
            FilterChip(
                selected = state.filterFavoritesOnly,
                onClick  = { viewModel.setFavoritesOnly(!state.filterFavoritesOnly) },
                label    = { Text("★ Favorites") },
            )

            Spacer(Modifier.weight(1f))

            // Sort button
            IconButton(onClick = { sortMenuExpanded = true }) {
                Icon(Icons.Default.Sort, contentDescription = "Sort")
            }
            DropdownMenu(
                expanded         = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
            ) {
                RecordingSortOrder.values().forEach { sort ->
                    DropdownMenuItem(
                        text    = { Text(sort.displayName()) },
                        onClick = { viewModel.setSortOrder(sort); sortMenuExpanded = false },
                        trailingIcon = if (state.sortOrder == sort) {
                            { Text("✓") }
                        } else null,
                    )
                }
            }
        }

        // ── Selection action bar ───────────────────────────────────────────
        if (state.isInSelectionMode) {
            Row(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically,
            ) {
                Text(
                    "${state.selectedIds.size} selected",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row {
                    IconButton(onClick = viewModel::requestDeleteSelected) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = viewModel::clearSelection) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                    }
                }
            }
        }

        // ── Recordings list ────────────────────────────────────────────────
        if (state.recordings.isEmpty() && !state.isLoading) {
            EmptyStateView(
                icon     = Icons.Default.GraphicEq,
                title    = if (state.searchQuery.isNotBlank()) "No results found" else "No recordings yet",
                subtitle = if (state.searchQuery.isNotBlank()) "Try a different search term"
                           else "Recordings will appear here automatically after calls",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier       = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.recordings, key = { it.id }) { recording ->
                    RecordingListItem(
                        recording        = recording,
                        onClick          = {
                            if (state.isInSelectionMode) viewModel.toggleSelection(recording.id)
                            else onRecordingClick(recording.id)
                        },
                        onFavoriteToggle = { viewModel.toggleFavorite(recording.id) },
                        onDelete         = { viewModel.deleteRecording(recording.id) },
                        onRename         = { viewModel.showRenameDialog(recording.id, recording.displayName) },
                        onShare          = { /* share intent */ },
                        modifier         = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────
    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title            = { Text("Delete ${state.selectedIds.size} recordings?") },
            text             = { Text("This action cannot be undone. The audio files will be permanently deleted.") },
            confirmButton    = {
                TextButton(onClick = viewModel::deleteSelected) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton    = {
                TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") }
            },
        )
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
                    onClick  = { viewModel.renameRecording(nameText) },
                    enabled  = nameText.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton    = {
                TextButton(onClick = viewModel::dismissRenameDialog) { Text("Cancel") }
            },
        )
    }
}

private fun RecordingSortOrder.displayName() = when (this) {
    RecordingSortOrder.NEWEST_FIRST   -> "Newest first"
    RecordingSortOrder.OLDEST_FIRST   -> "Oldest first"
    RecordingSortOrder.LONGEST_FIRST  -> "Longest first"
    RecordingSortOrder.SHORTEST_FIRST -> "Shortest first"
    RecordingSortOrder.BY_NAME        -> "By name"
    RecordingSortOrder.LARGEST_FIRST  -> "Largest first"
}
