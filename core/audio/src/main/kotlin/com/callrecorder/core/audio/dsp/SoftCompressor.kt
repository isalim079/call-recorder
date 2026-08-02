package com.callrecorder.core.audio.dsp

/**
 * Soft knee compressor / limiter for speech leveling.
 * Raises quiet talk, tames peaks without hard clipping.
 */
class SoftCompressor(
    private val threshold: Float = 0.35f,
    private val ratio: Float = 4f,
    private val makeup: Float = 1.35f,
) {
    private var envelope = 0f
    private val attack = 0.15f
    private val release = 0.02f

    fun process(samples: FloatArray) {
        for (i in samples.indices) {
            val abs = kotlin.math.abs(samples[i])
            envelope = if (abs > envelope) {
                envelope + attack * (abs - envelope)
            } else {
                envelope + release * (abs - envelope)
            }

            val gain = if (envelope > threshold) {
                // Soft knee: y = T * (x/T)^(1/R)  →  gain = y/x
                val compressed =
                    threshold * Math.pow((envelope / threshold).toDouble(), 1.0 / ratio).toFloat()
                (compressed / envelope).coerceIn(0.12f, 1f)
            } else {
                1f
            }

            var out = samples[i] * gain * makeup
            // Soft clip — avoid hard digital limiting
            out = kotlin.math.tanh(out.toDouble()).toFloat()
            samples[i] = out
        }
    }

    fun reset() {
        envelope = 0f
    }
}
