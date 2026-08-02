package com.callrecorder.core.audio.effects

import android.media.AudioRecord
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import timber.log.Timber

/**
 * Hardware / OS-level audio effects attached to an [AudioRecord] session.
 * Availability is OEM-dependent — best-effort, never throws to callers.
 */
class AndroidAudioEffects {

    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var agc: AutomaticGainControl? = null

    data class Status(
        val noiseSuppressor: Boolean,
        val echoCanceler: Boolean,
        val automaticGainControl: Boolean,
    )

    fun attach(audioRecord: AudioRecord): Status {
        release()
        val sessionId = audioRecord.audioSessionId
        var ns = false
        var aec = false
        var gain = false

        if (NoiseSuppressor.isAvailable()) {
            try {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.also {
                    it.enabled = true
                    ns = true
                }
            } catch (e: Exception) {
                Timber.w(e, "NoiseSuppressor attach failed")
            }
        }

        if (AcousticEchoCanceler.isAvailable()) {
            try {
                echoCanceler = AcousticEchoCanceler.create(sessionId)?.also {
                    it.enabled = true
                    aec = true
                }
            } catch (e: Exception) {
                Timber.w(e, "AcousticEchoCanceler attach failed")
            }
        }

        if (AutomaticGainControl.isAvailable()) {
            try {
                agc = AutomaticGainControl.create(sessionId)?.also {
                    it.enabled = true
                    gain = true
                }
            } catch (e: Exception) {
                Timber.w(e, "AutomaticGainControl attach failed")
            }
        }

        Timber.d("Audio effects: NS=$ns AEC=$aec AGC=$gain session=$sessionId")
        return Status(ns, aec, gain)
    }

    fun release() {
        try { noiseSuppressor?.release() } catch (_: Exception) {}
        try { echoCanceler?.release() } catch (_: Exception) {}
        try { agc?.release() } catch (_: Exception) {}
        noiseSuppressor = null
        echoCanceler = null
        agc = null
    }
}
