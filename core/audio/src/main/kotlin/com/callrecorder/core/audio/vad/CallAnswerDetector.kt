package com.callrecorder.core.audio.vad

import com.callrecorder.core.audio.dsp.NoiseReductionPipeline

/**
 * Detects likely call answer (speech / energy rising above ringback).
 * Lenient: energy above floor is enough if sustained — call mics are noisy.
 */
class CallAnswerDetector(
    private val requiredSpeechFrames: Int = 6, // ~120ms
    private val energyRatio: Float = 2.0f,
    private val minSpeechRms: Float = 0.006f,
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

    fun onFrame(samples: FloatArray): Boolean {
        if (answered) return false
        framesSeen++

        val rms = NoiseReductionPipeline.rms(samples)
        if (rms < noiseFloor * 1.5f || framesSeen < 5) {
            noiseFloor = noiseFloor * 0.9f + rms * 0.1f
            noiseFloor = noiseFloor.coerceIn(0.0003f, 0.05f)
        }

        val energyOk = rms >= maxOf(minSpeechRms, noiseFloor * energyRatio)
        // Soft voice check — do not require strict tonal rejection (missed real calls)
        val voiceBias = roughVoiceBias(samples, rms)

        if (energyOk && voiceBias) {
            speechRun++
        } else if (energyOk) {
            speechRun++ // energy alone still counts, slower
            if (speechRun < requiredSpeechFrames / 2) speechRun++
        } else {
            speechRun = maxOf(0, speechRun - 1)
        }

        if (speechRun >= requiredSpeechFrames) {
            answered = true
            return true
        }
        return false
    }

    private fun roughVoiceBias(samples: FloatArray, rms: Float): Boolean {
        if (samples.size < 32 || rms < 1e-6f) return false
        var zc = 0
        for (i in 1 until samples.size) {
            if ((samples[i - 1] >= 0) != (samples[i] >= 0)) zc++
        }
        val zcr = zc.toFloat() / samples.size
        return zcr in 0.01f..0.45f
    }
}
