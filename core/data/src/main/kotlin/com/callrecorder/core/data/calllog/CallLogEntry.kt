package com.callrecorder.core.data.calllog

data class CallLogEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val type: Int, // CallLog.Calls.INCOMING_TYPE etc.
    val dateMs: Long,
    val durationSec: Long,
    val photoUri: String? = null,
)
