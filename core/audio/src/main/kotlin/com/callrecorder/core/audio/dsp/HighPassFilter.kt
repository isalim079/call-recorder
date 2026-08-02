package com.callrecorder.core.audio.dsp

/**
 * 2nd-order high-pass biquad (Direct Form I). Removes rumble / DC / wind-like low noise.
 */
class HighPassFilter(
    sampleRate: Int,
    cutoffHz: Float = 80f,
    q: Float = 0.707f,
) {
    private val b0: Float
    private val b1: Float
    private val b2: Float
    private val a1: Float
    private val a2: Float

    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    init {
        val w0 = (2.0 * Math.PI * cutoffHz / sampleRate).toFloat()
        val cosW = kotlin.math.cos(w0)
        val sinW = kotlin.math.sin(w0)
        val alpha = sinW / (2f * q)
        val a0 = 1f + alpha
        b0 = ((1f + cosW) / 2f) / a0
        b1 = (-(1f + cosW)) / a0
        b2 = ((1f + cosW) / 2f) / a0
        a1 = (-2f * cosW) / a0
        a2 = (1f - alpha) / a0
    }

    fun process(samples: FloatArray) {
        for (i in samples.indices) {
            val x = samples[i]
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x
            y2 = y1; y1 = y
            samples[i] = y
        }
    }

    fun reset() {
        x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
    }
}
