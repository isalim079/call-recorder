package com.callrecorder.app.phone

import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Bridge between [android.telecom.InCallService] and Compose InCall UI.
 * Pattern used by AOSP Dialer: service owns Call objects, UI issues actions.
 */
object ActiveCallController {

    data class CallSnapshot(
        val id: String,
        val number: String,
        val state: Int,
        val isIncoming: Boolean,
        val isMuted: Boolean = false,
        val isSpeaker: Boolean = false,
    )

    private var serviceRef: WeakReference<InCallService>? = null
    private val callMap = LinkedHashMap<String, Call>()

    private val _calls = MutableStateFlow<List<CallSnapshot>>(emptyList())
    val calls: StateFlow<List<CallSnapshot>> = _calls.asStateFlow()

    private val _primary = MutableStateFlow<CallSnapshot?>(null)
    val primary: StateFlow<CallSnapshot?> = _primary.asStateFlow()

    fun bindService(service: InCallService) {
        serviceRef = WeakReference(service)
    }

    fun unbindService() {
        serviceRef = null
    }

    fun onCallAdded(call: Call) {
        val id = idOf(call)
        callMap[id] = call
        publish()
    }

    fun onCallRemoved(call: Call) {
        callMap.remove(idOf(call))
        publish()
    }

    fun onStateChanged(call: Call) {
        val id = idOf(call)
        if (callMap.containsKey(id)) {
            callMap[id] = call
            publish()
        }
    }

    fun answer() {
        primaryCall()?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun reject() {
        primaryCall()?.reject(false, null)
    }

    fun disconnect() {
        primaryCall()?.disconnect()
    }

    fun hold(hold: Boolean) {
        val c = primaryCall() ?: return
        if (hold) c.hold() else c.unhold()
    }

    fun setMuted(muted: Boolean) {
        serviceRef?.get()?.setMuted(muted)
        publish()
    }

    fun setSpeaker(speaker: Boolean) {
        val svc = serviceRef?.get() ?: return
        try {
            // Route: 1=earpiece, 8=speaker (CallAudioState.ROUTE_SPEAKER)
            val route = if (speaker) android.telecom.CallAudioState.ROUTE_SPEAKER
            else android.telecom.CallAudioState.ROUTE_EARPIECE
            svc.setAudioRoute(route)
        } catch (e: Exception) {
            Timber.w(e, "setAudioRoute failed")
        }
        publish()
    }

    fun playDtmf(digit: Char) {
        primaryCall()?.playDtmfTone(digit)
    }

    fun stopDtmf() {
        primaryCall()?.stopDtmfTone()
    }

    private fun primaryCall(): Call? {
        // Prefer ringing, then active, then first
        callMap.values.firstOrNull { it.state == Call.STATE_RINGING }?.let { return it }
        callMap.values.firstOrNull { it.state == Call.STATE_ACTIVE }?.let { return it }
        return callMap.values.firstOrNull()
    }

    private fun publish() {
        val svc = serviceRef?.get()
        val audio = try {
            svc?.callAudioState
        } catch (_: Exception) {
            null
        }
        val snaps = callMap.map { (id, call) ->
            val number = call.details?.handle?.schemeSpecificPart.orEmpty()
            val incoming = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    call.details?.callDirection == Call.Details.DIRECTION_INCOMING
                } else false
            } catch (_: Exception) {
                call.state == Call.STATE_RINGING
            }
            CallSnapshot(
                id = id,
                number = number,
                state = call.state,
                isIncoming = incoming,
                isMuted = audio?.isMuted == true,
                isSpeaker = (audio?.route ?: 0) and android.telecom.CallAudioState.ROUTE_SPEAKER != 0,
            )
        }
        _calls.value = snaps
        _primary.value = snaps.firstOrNull { it.state == Call.STATE_RINGING }
            ?: snaps.firstOrNull { it.state == Call.STATE_ACTIVE }
            ?: snaps.firstOrNull()
    }

    private fun idOf(call: Call): String =
        System.identityHashCode(call).toString()
}
