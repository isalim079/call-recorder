package com.callrecorder.core.audio.vad

import com.callrecorder.core.audio.dsp.NoiseReductionPipeline
import kotlin.math.sqrt

/**
 * Detects when remote party actually answers (speech starts), so ringback is not recorded.
 *
 * Heuristics (tuned for dial-tone / ringback rejection):
 * - Energy above adaptive noise floor for [requiredSpeechFrames] consecutive frames
 * - Broadband voice-band (rough mid-band energy) vs pure tones
 * - Minimum hangover so a single ringburst cannot trip
 */
class CallAnswerDetector(
    private val requiredSpeechFrames: Int = 12, // ~240ms at 20ms frames
    private val energyRatio: Float = 3.2f,
    private val minSpeechRms: Float = 0.012f,
) {
    private var noiseFloor = 0.004f
    private var speechRun = 0
    private var answered = false
    private var framesSeen = 0

    val hasAnswered: Boolean get() = answered

    fun reset() {
        noiseFloor = 0.004f
        speechRun = 0
        answered = false
        framesSeen = 0
    }

    /**
     * Feed one frame of mono floats [-1, 1].
     * @return true on the frame when answer is first detected.
     */
    fun onFrame(samples: FloatArray): Boolean {
        if (answered) return false
        framesSeen++

        val rms = NoiseReductionPipeline.rms(samples)
        // Slow noise floor track while quiet
        if (rms < noiseFloor * 1.5f || framesSeen < 8) {
            noiseFloor = noiseFloor * 0.9f + rms * 0.1f
            noiseFloor = noiseFloor.coerceIn(0.0005f, 0.05f)
        }

        val energyOk = rms >= maxOf(minSpeechRms, noiseFloor * energyRatio)
        val voiceLike = isVoiceLike(samples, rms)

        if (energyOk && voiceLike) {
            speechRun++
        } else {
            speechRun = maxOf(0, speechRun - 2)
        }

        if (speechRun >= requiredSpeechFrames) {
            answered = true
            return true
        }
        return false
    }

    /**
     * Reject pure ring tones: look at zero-crossing rate + RMS variance proxies.
     * Speech has moderate ZCR; sine ring has stable ZCR and flat envelope.
     */
    private fun isVoiceLike(samples: FloatArray, rms: Float): Boolean {
        if (samples.size < 32 || rms < 1e-5f) return false

        var zc = 0
        for (i in 1 until samples.size) {
            if ((samples[i - 1] >= 0 && samples[i] < 0) || (samples[i - 1] < 0 && samples[i] >= 0)) {
                zc++
            }
        }
        val zcr = zc.toFloat() / samples.size

        // Envelope modulation (speech has more AM than pure ringback)
        val half = samples.size / 2
        var e1 = 0.0
        var e2 = 0.0
        for (i in 0 until half) e1 += samples[i] * samples[i]
        for (i in half until samples.size) e2 += samples[i] * samples[i]
        e1 = sqrt(e1 / half)
        e2 = sqrt(e2 / (samples.size - half))
        val modulation = kotlin.math.abs(e1 - e2) / (e1 + e2 + 1e-6)

        // Typical speech ZCR rough band 0.02–0.25 of samples; modulation higher than pure tone
        val zcrOk = zcr in 0.015f..0.35f
        val modOk = modulation > 0.04 || zcr in 0.04f..0.25f
        return zcrOk && modOk
    }
}
