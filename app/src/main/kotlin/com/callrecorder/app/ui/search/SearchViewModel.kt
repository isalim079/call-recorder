package com.callrecorder.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.core.domain.model.Recording
import com.callrecorder.core.domain.usecase.DeleteRecordingUseCase
import com.callrecorder.core.domain.usecase.RenameRecordingUseCase
import com.callrecorder.core.domain.usecase.SearchRecordingsUseCase
import com.callrecorder.core.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Recording> = emptyList(),
    val isSearching: Boolean = false,
    // Rename dialog state
    val showRenameDialog: Boolean = false,
    val renameTargetId: Long? = null,
    val renameInitialName: String = "",
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRecordingsUseCase: SearchRecordingsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteRecordingUseCase: DeleteRecordingUseCase,
    private val renameRecordingUseCase: RenameRecordingUseCase,
) : ViewModel() {

    private val _query       = MutableStateFlow("")
    private val _renameDialog = MutableStateFlow<Pair<Long, String>?>(null)

    val uiState: StateFlow<SearchUiState> = combine(
        _query
            .debounce(300)
            .flatMapLatest { q ->
                if (q.isBlank()) flowOf(emptyList())
                else searchRecordingsUseCase(q)
            },
        _query,
        _renameDialog,
    ) { results, q, renameDialog ->
        SearchUiState(
            query            = q,
            results          = results,
            isSearching      = q.isNotBlank() && results.isEmpty(),
            showRenameDialog = renameDialog != null,
            renameTargetId   = renameDialog?.first,
            renameInitialName = renameDialog?.second ?: "",
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    fun setQuery(q: String) { _query.value = q }
    fun clearQuery() { _query.value = "" }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { toggleFavoriteUseCase(id) }
    }

    fun deleteRecording(id: Long) {
        viewModelScope.launch { deleteRecordingUseCase(id) }
    }

    fun showRenameDialog(id: Long, currentName: String) {
        _renameDialog.update { id to currentName }
    }

    fun dismissRenameDialog() {
        _renameDialog.value = null
    }

    fun renameRecording(newName: String) {
        val id = _renameDialog.value?.first ?: return
        viewModelScope.launch {
            runCatching { renameRecordingUseCase(id, newName) }
            _renameDialog.value = null
        }
    }
}
