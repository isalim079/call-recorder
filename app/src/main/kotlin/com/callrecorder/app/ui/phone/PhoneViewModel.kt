package com.callrecorder.app.ui.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.core.data.calllog.CallLogEntry
import com.callrecorder.core.data.calllog.CallLogRepository
import com.callrecorder.core.data.contacts.ContactEntry
import com.callrecorder.core.data.contacts.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PhoneUiState(
    val recents: List<CallLogEntry> = emptyList(),
    val contacts: List<ContactEntry> = emptyList(),
    val dialpadMatches: List<ContactEntry> = emptyList(),
    val contactQuery: String = "",
    val loading: Boolean = true,
)

@HiltViewModel
class PhoneViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val callLogRepository: CallLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneUiState())
    val uiState: StateFlow<PhoneUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var dialpadJob: Job? = null

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val recents = withContext(Dispatchers.IO) { callLogRepository.getRecent(100) }
            val contacts = withContext(Dispatchers.IO) { contactsRepository.searchContacts("", 120) }
            _uiState.update {
                it.copy(recents = recents, contacts = contacts, loading = false)
            }
        }
    }

    fun searchContacts(query: String) {
        _uiState.update { it.copy(contactQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotEmpty()) delay(180)
            val results = withContext(Dispatchers.IO) {
                contactsRepository.searchContacts(query, 120)
            }
            _uiState.update { it.copy(contacts = results) }
        }
    }

    fun filterDialpad(number: String) {
        dialpadJob?.cancel()
        if (number.isBlank()) {
            _uiState.update { it.copy(dialpadMatches = emptyList()) }
            return
        }
        dialpadJob = viewModelScope.launch {
            delay(120)
            val results = withContext(Dispatchers.IO) {
                contactsRepository.searchContacts(number, 12)
            }
            _uiState.update { it.copy(dialpadMatches = results) }
        }
    }
}
