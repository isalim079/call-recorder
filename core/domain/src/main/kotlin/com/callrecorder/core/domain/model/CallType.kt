package com.callrecorder.core.domain.model

/**
 * Represents the direction of a recorded call.
 *
 * Used throughout the domain, UI, and database layers.
 */
enum class CallType {
    /** The call was received on this device. */
    INCOMING,
    /** The call was placed from this device. */
    OUTGOING,
    /** Direction could not be determined (legacy data or restricted API). */
    UNKNOWN,
}
