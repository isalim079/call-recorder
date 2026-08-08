package com.callrecorder.core.data.contacts

data class ContactEntry(
    val id: Long,
    val displayName: String,
    val phoneNumber: String,
    val photoUri: String?,
    val normalizedNumber: String? = null,
)
