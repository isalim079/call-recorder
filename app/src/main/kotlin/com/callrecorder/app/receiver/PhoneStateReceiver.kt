package com.callrecorder.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.callrecorder.app.service.CallEvent
import com.callrecorder.app.service.CallRecorderService
import com.callrecorder.app.service.CallStateManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Listens for phone state changes from the Android telephony system.
 *
 * Annotated with [@AndroidEntryPoint] to allow Hilt to inject [CallStateManager]
 * directly. This enables processing states in the receiver without starting
 * the background service unless we are ready to record, avoiding background service
 * start limitations on modern Android versions.
 */
@AndroidEntryPoint
class PhoneStateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var callStateManager: CallStateManager

    override fun onReceive(context: Context, intent: Intent) {
        val actionStr = intent.action ?: return

        // Intercept outgoing call initialization to capture the number
        if (actionStr == Intent.ACTION_NEW_OUTGOING_CALL) {
            val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: return
            Timber.d("PhoneStateReceiver: new outgoing call to $number")
            callStateManager.setOutgoingNumber(number)
            return
        }

        if (actionStr != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Timber.d("PhoneStateReceiver: state=$state number=$number")

        // Delegate state mapping to the CallStateManager singleton
        val event = callStateManager.onPhoneStateChanged(state, number)
        when (event) {
            is CallEvent.StartRecording -> {
                Timber.d("PhoneStateReceiver: initiating foreground recording service")
                val serviceIntent = Intent(context, CallRecorderService::class.java).apply {
                    action = CallRecorderService.ACTION_START_RECORDING
                    putExtra(CallRecorderService.EXTRA_PHONE_NUMBER, event.phoneNumber)
                    putExtra(CallRecorderService.EXTRA_CALL_DIRECTION, event.direction.name)
                }
                try {
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start CallRecorderService for start recording")
                }
            }
            is CallEvent.StopRecording -> {
                Timber.d("PhoneStateReceiver: stopping foreground recording service")
                val serviceIntent = Intent(context, CallRecorderService::class.java).apply {
                    action = CallRecorderService.ACTION_STOP_RECORDING
                    putExtra(CallRecorderService.EXTRA_PHONE_NUMBER, event.phoneNumber)
                }
                try {
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start CallRecorderService for stop recording")
                }
            }
            is CallEvent.None -> {
                // No action needed for transitional states (e.g. RINGING)
            }
        }
    }
}
