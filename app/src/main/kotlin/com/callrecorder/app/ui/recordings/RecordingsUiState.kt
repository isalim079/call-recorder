package com.callrecorder.app.ui.recordings

import com.callrecorder.core.domain.model.CallType
import com.callrecorder.core.domain.model.Recording
import com.callrecorder.core.domain.model.RecordingSortOrder

data class RecordingsUiState(
    val isLoading: Boolean = true,
    val recordings: List<Recording> = emptyList(),
    val sortOrder: RecordingSortOrder = RecordingSortOrder.NEWEST_FIRST,
    val filterCallType: CallType? = null,
    val filterFavoritesOnly: Boolean = false,
    val searchQuery: String = "",
    val selectedIds: Set<Long> = emptySet(),
    val isInSelectionMode: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showRenameDialog: Boolean = false,
    val renameTargetId: Long? = null,
    val renameInitialName: String = "",
)
