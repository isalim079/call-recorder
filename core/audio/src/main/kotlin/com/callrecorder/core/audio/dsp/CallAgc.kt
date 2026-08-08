package com.callrecorder.core.audio.dsp

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Telephony AGC + peak limiter for call PCM (float −1…1).
 */
class CallAgc(
    private val targetRms: Float = 0.18f,
    private val maxGain: Float = 12f,
    private val minGain: Float = 0.35f,
    private val attack: Float = 0.12f,
    private val release: Float = 0.02f,
) {
    private var gain = 1.5f
    private var envelope = 0.02f

    fun reset() {
        gain = 1.5f
        envelope = 0.02f
    }

    fun process(samples: FloatArray) {
        if (samples.isEmpty()) return
        var sum = 0.0
        var peak = 0f
        for (s in samples) {
            val a = abs(s)
            sum += (s * s).toDouble()
            if (a > peak) peak = a
        }
        val rms = sqrt(sum / samples.size).toFloat().coerceAtLeast(1e-6f)
        envelope = if (rms > envelope) {
            envelope + attack * (rms - envelope)
        } else {
            envelope + release * (rms - envelope)
        }

        val desired = (targetRms / envelope.coerceAtLeast(1e-5f)).coerceIn(minGain, maxGain)
        gain += 0.15f * (desired - gain)

        val peakAfter = peak * gain
        val peakScale = if (peakAfter > 0.92f) 0.92f / peakAfter else 1f
        val g = gain * peakScale

        for (i in samples.indices) {
            var y = samples[i] * g
            y = tanhSoft(y)
            samples[i] = y
        }
    }

    private fun tanhSoft(x: Float): Float {
        return if (abs(x) < 0.7f) x
        else {
            val s = if (x >= 0) 1f else -1f
            s * (0.95f * kotlin.math.tanh(abs(x).toDouble() * 1.2).toFloat())
        }
    }
}

object LoudnessNormalizer {
    fun normalizeInPlace(samples: FloatArray, targetPeak: Float = 0.89f, targetRms: Float = 0.16f) {
        if (samples.isEmpty()) return
        var peak = 1e-6f
        var sum = 0.0
        for (s in samples) {
            val a = abs(s)
            if (a > peak) peak = a
            sum += (s * s).toDouble()
        }
        val rms = sqrt(sum / samples.size).toFloat().coerceAtLeast(1e-6f)
        val peakGain = targetPeak / peak
        val rmsGain = targetRms / rms
        var g = min(peakGain, max(rmsGain, peakGain * 0.55f))
        g = g.coerceIn(0.5f, 10f)
        for (i in samples.indices) {
            var y = samples[i] * g
            if (abs(y) > 0.95f) y = 0.95f * (if (y >= 0) 1 else -1)
            samples[i] = y
        }
    }

    class Streaming(
        private val targetRms: Float = 0.17f,
        private val maxGain: Float = 8f,
    ) {
        private var gain = 2f
        fun process(block: FloatArray) {
            var sum = 0.0
            var peak = 1e-6f
            for (s in block) {
                sum += (s * s).toDouble()
                peak = max(peak, abs(s))
            }
            val rms = sqrt(sum / block.size.coerceAtLeast(1)).toFloat().coerceAtLeast(1e-5f)
            val desired = (targetRms / rms).coerceIn(0.4f, maxGain)
            gain = 0.9f * gain + 0.1f * desired
            val peakCap = if (peak * gain > 0.9f) 0.9f / peak else gain
            val g = min(gain, peakCap)
            for (i in block.indices) {
                block[i] = (block[i] * g).coerceIn(-0.95f, 0.95f)
            }
        }
    }
}
