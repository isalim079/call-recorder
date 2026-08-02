package com.callrecorder.core.audio.dsp

/**
 * Adaptive downward expander / gate.
 * Frames below the moving noise floor are attenuated; speech passes.
 */
class AdaptiveNoiseGate(
    private val openRatio: Float = 2.8f,
    private val attenuate: Float = 0.08f,
    private val attack: Float = 0.35f,
    private val release: Float = 0.08f,
) {
    private var noiseFloor = 0.003f
    private var gain = 1f

    fun learn(rms: Float) {
        noiseFloor = noiseFloor * 0.92f + rms * 0.08f
        noiseFloor = noiseFloor.coerceIn(0.0004f, 0.08f)
    }

    fun process(samples: FloatArray, rms: Float) {
        val target = if (rms > noiseFloor * openRatio) 1f else attenuate
        gain += if (target > gain) attack * (target - gain) else release * (target - gain)
        for (i in samples.indices) {
            samples[i] *= gain
        }
    }

    fun noiseFloor(): Float = noiseFloor

    fun reset() {
        noiseFloor = 0.003f
        gain = 1f
    }
}
