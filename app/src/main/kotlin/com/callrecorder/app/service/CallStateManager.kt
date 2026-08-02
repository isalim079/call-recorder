package com.callrecorder.app.service

import android.telephony.TelephonyManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks phone call state transitions and determines call direction.
 *
 * State machine:
 * ```
 *   IDLE ──ringing──► RINGING ──offhook──► CALL_ACTIVE ──idle──► IDLE
 *        ──offhook──► CALL_ACTIVE (outgoing)
 *   RINGING ──idle──► IDLE (missed / rejected)
 * ```
 *
 * Thread-safe: all state is written and read within synchronized blocks.
 */
@Singleton
class CallStateManager @Inject constructor() {

    enum class CallState { IDLE, RINGING, CALL_ACTIVE }
    enum class CallDirection { INCOMING, OUTGOING, UNKNOWN }

    @Volatile private var currentState: CallState = CallState.IDLE
    @Volatile private var direction: CallDirection = CallDirection.UNKNOWN
    @Volatile private var phoneNumber: String = ""

    /**
     * Handle a new phone state from [TelephonyManager.EXTRA_STATE].
     *
     * @param newState   One of [TelephonyManager.EXTRA_STATE_RINGING],
     *                   [TelephonyManager.EXTRA_STATE_OFFHOOK],
     *                   [TelephonyManager.EXTRA_STATE_IDLE].
     * @param number     Phone number (available only in RINGING on most devices).
     * @return [CallEvent] describing what should happen.
     */
    @Synchronized
    fun onPhoneStateChanged(newState: String, number: String?): CallEvent {
        Timber.d("PhoneState: $currentState → $newState (number=$number)")

        return when (newState) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                phoneNumber = number ?: ""
                direction = CallDirection.INCOMING
                currentState = CallState.RINGING
                CallEvent.None
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val wasRinging = currentState == CallState.RINGING
                if (!wasRinging) {
                    // No prior RINGING → outgoing call
                    direction = CallDirection.OUTGOING
                    if (!number.isNullOrBlank()) {
                        phoneNumber = number
                    }
                }
                currentState = CallState.CALL_ACTIVE
                CallEvent.StartRecording(phoneNumber, direction)
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                val wasActive = currentState == CallState.CALL_ACTIVE
                currentState = CallState.IDLE
                direction = CallDirection.UNKNOWN
                val num = phoneNumber
                phoneNumber = ""
                if (wasActive) CallEvent.StopRecording(num)
                else CallEvent.None   // Missed / rejected — never answered
            }

            else -> CallEvent.None
        }
    }

    @Synchronized
    fun setOutgoingNumber(number: String) {
        if (currentState == CallState.IDLE) {
            phoneNumber = number
            direction = CallDirection.OUTGOING
        }
    }

    fun getCurrentState(): CallState = currentState
    fun getCurrentPhoneNumber(): String = phoneNumber
    fun getCurrentDirection(): CallDirection = direction

    fun reset() {
        currentState = CallState.IDLE
        direction = CallDirection.UNKNOWN
        phoneNumber = ""
    }
}

/** Describes what the service should do in response to a phone state change. */
sealed class CallEvent {
    object None : CallEvent()
    data class StartRecording(val phoneNumber: String, val direction: CallStateManager.CallDirection) : CallEvent()
    data class StopRecording(val phoneNumber: String) : CallEvent()
}
