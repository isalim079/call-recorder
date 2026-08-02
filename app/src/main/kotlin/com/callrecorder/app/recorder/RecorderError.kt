package com.callrecorder.app.recorder

/**
 * Represents errors that can occur during audio recording.
 * Returned as [Result.Failure] from [AudioRecorderEngine] methods.
 */
sealed class RecorderError : Exception() {
    /** RECORD_AUDIO or READ_PHONE_STATE permission was denied. */
    object PermissionDenied : RecorderError()

    /** The microphone is busy (another app is recording). */
    object MicrophoneBusy : RecorderError()

    /** Device storage is full — cannot write the recording file. */
    object StorageFull : RecorderError()

    /** MediaRecorder entered an invalid state. */
    data class InvalidState(override val message: String?) : RecorderError()

    /** Generic unexpected error. */
    data class Unknown(override val message: String?, override val cause: Throwable? = null) : RecorderError()
}
