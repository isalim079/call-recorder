package com.callrecorder.app.service

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import androidx.core.content.ContextCompat
import com.callrecorder.app.phone.ActiveCallController
import com.callrecorder.app.ui.phone.InCallActivity
import timber.log.Timber

/**
 * Telecom InCallService: call lifecycle + binding for full dialer UI.
 * Starts recording on ACTIVE; presents [InCallActivity] for answer/hangup.
 * Never injects announcement audio into the call stream.
 */
class CallInCallService : InCallService() {

    private val callbacks = HashMap<Call, Call.Callback>()
    private var activeCall: Call? = null

    override fun onCreate() {
        super.onCreate()
        ActiveCallController.bindService(this)
    }

    override fun onDestroy() {
        ActiveCallController.unbindService()
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Timber.i("InCallService onCallAdded state=${call.state}")
        ActiveCallController.onCallAdded(call)

        val cb = object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                ActiveCallController.onStateChanged(c)
                handleState(c, state)
            }
        }
        callbacks[call] = cb
        call.registerCallback(cb)
        launchInCallUi()
        handleState(call, call.state)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Timber.i("InCallService onCallRemoved")
        callbacks.remove(call)?.let { call.unregisterCallback(it) }
        ActiveCallController.onCallRemoved(call)
        if (activeCall === call) {
            activeCall = null
            stopRecording(numberOf(call))
        }
    }

    private fun launchInCallUi() {
        try {
            val intent = Intent(this, InCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open InCallActivity")
        }
    }

    private fun handleState(call: Call, state: Int) {
        when (state) {
            Call.STATE_ACTIVE -> {
                activeCall = call
                startRecording(call)
            }
            Call.STATE_DISCONNECTED -> {
                if (activeCall === call || activeCall == null) {
                    activeCall = null
                    stopRecording(numberOf(call))
                }
            }
        }
    }

    private fun startRecording(call: Call) {
        val number = numberOf(call)
        val direction = directionOf(call)
        Timber.i("InCall ACTIVE → start record number=$number dir=$direction")
        val intent = Intent(this, CallRecorderService::class.java).apply {
            action = CallRecorderService.ACTION_START_RECORDING
            putExtra(CallRecorderService.EXTRA_PHONE_NUMBER, number)
            putExtra(CallRecorderService.EXTRA_CALL_DIRECTION, direction.name)
        }
        try {
            ContextCompat.startForegroundService(this, intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start CallRecorderService from InCallService")
        }
    }

    private fun stopRecording(number: String) {
        Timber.i("InCall end → stop record number=$number")
        val intent = Intent(this, CallRecorderService::class.java).apply {
            action = CallRecorderService.ACTION_STOP_RECORDING
            putExtra(CallRecorderService.EXTRA_PHONE_NUMBER, number)
        }
        try {
            ContextCompat.startForegroundService(this, intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop CallRecorderService from InCallService")
        }
    }

    private fun directionOf(call: Call): CallStateManager.CallDirection {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return when (call.details?.callDirection) {
                Call.Details.DIRECTION_INCOMING -> CallStateManager.CallDirection.INCOMING
                Call.Details.DIRECTION_OUTGOING -> CallStateManager.CallDirection.OUTGOING
                else -> CallStateManager.CallDirection.UNKNOWN
            }
        }
        return CallStateManager.CallDirection.UNKNOWN
    }

    private fun numberOf(call: Call): String =
        call.details?.handle?.schemeSpecificPart.orEmpty()
}
