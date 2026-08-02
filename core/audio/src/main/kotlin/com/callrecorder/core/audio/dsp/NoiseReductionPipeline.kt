package com.callrecorder.core.audio.dsp

/**
 * Full software denoise chain for call audio PCM.
 *
 * Order:
 * 1. High-pass — dump low-frequency junk
 * 2. Spectral subtraction — remove learned ambient / ring noise profile
 * 3. Adaptive gate — cut residual hush
 * 4. Soft compressor — level speech for HD clarity
 */
class NoiseReductionPipeline(
    sampleRate: Int,
    fftSize: Int = 512,
) {
    private val highPass = HighPassFilter(sampleRate)
    private val spectral = SpectralNoiseSubtractor(fftSize = fftSize, hopSize = fftSize / 2)
    private val gate = AdaptiveNoiseGate()
    private val compressor = SoftCompressor()

    val isLearningNoise: Boolean get() = spectral.isLearning
    val noiseFramesLearned: Int get() = spectral.noiseFramesLearned

    fun learnFrame(samples: FloatArray) {
        // Only high-pass while learning so noise profile is cleaner
        highPass.process(samples)
        spectral.process(samples) // accumulates while learning
        gate.learn(rms(samples))
    }

    fun lockNoiseProfile() {
        spectral.freezeNoiseProfile()
    }

    /**
     * Denoise one PCM block in place (input floats -1..1).
     * Spectral stage may reduce length slightly during warm-up; returns processed buffer.
     */
    fun process(samples: FloatArray): FloatArray {
        highPass.process(samples)
        val cleaned = spectral.process(samples)
        val target = if (cleaned.isNotEmpty()) cleaned else samples
        val r = rms(target)
        gate.process(target, r)
        compressor.process(target)
        return target
    }

    fun reset() {
        highPass.reset()
        spectral.reset()
        gate.reset()
        compressor.reset()
    }

    companion object {
        fun rms(samples: FloatArray): Float {
            if (samples.isEmpty()) return 0f
            var sum = 0.0
            for (s in samples) sum += s * s
            return kotlin.math.sqrt(sum / samples.size).toFloat()
        }

        fun shortsToFloat(src: ShortArray, count: Int, dst: FloatArray) {
            val n = minOf(count, dst.size, src.size)
            for (i in 0 until n) {
                dst[i] = src[i] / 32768f
            }
        }

        fun floatToShorts(src: FloatArray, dst: ShortArray): Int {
            val n = minOf(src.size, dst.size)
            for (i in 0 until n) {
                val v = (src[i] * 32767f).coerceIn(-32768f, 32767f)
                dst[i] = v.toInt().toShort()
            }
            return n
        }
    }
}
