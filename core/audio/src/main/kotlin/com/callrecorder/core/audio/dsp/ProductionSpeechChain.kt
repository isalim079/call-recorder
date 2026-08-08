package com.callrecorder.core.audio.dsp

/**
 * Production speech chain for call PCM (float −1…1 mono).
 *
 * 1. High-pass — rumble / DC / wind
 * 2. Wiener STFT denoise — hiss / ambient / ring tails
 * 3. Adaptive gate — residual room hush
 * 4. Soft compressor + speech presence — even HD playback
 */
class ProductionSpeechChain(
    sampleRate: Int,
    fftSize: Int = 512,
) {
    private val highPass = HighPassFilter(sampleRate, cutoffHz = 90f)
    private val wiener = WienerSpectralDenoiser(fftSize = fftSize, hopSize = fftSize / 2)
    private val gate = AdaptiveNoiseGate(
        openRatio = 2.2f,
        attenuate = 0.12f,
        attack = 0.4f,
        release = 0.06f,
    )
    private val compressor = SoftCompressor(
        threshold = 0.32f,
        ratio = 3.5f,
        makeup = 1.45f,
    )

    fun reset() {
        highPass.reset()
        wiener.reset()
        gate.reset()
        compressor.reset()
    }

    fun process(samples: FloatArray): FloatArray {
        highPass.process(samples)
        val denoised = wiener.process(samples)
        val target = if (denoised.isNotEmpty()) denoised else samples
        val r = NoiseReductionPipeline.rms(target)
        // Track floor only when quiet — preserve speech peaks
        if (r < gate.noiseFloor() * 1.6f) {
            gate.learn(r)
        }
        gate.process(target, r)
        compressor.process(target)
        applyPresence(target)
        return target
    }

    fun flush(): FloatArray {
        val tail = wiener.flush()
        if (tail.isEmpty()) return tail
        val r = NoiseReductionPipeline.rms(tail)
        gate.process(tail, r)
        compressor.process(tail)
        applyPresence(tail)
        return tail
    }

    private fun applyPresence(samples: FloatArray) {
        // Soft-knee sample gain ~ +1.5 dB when |x| in speech band magnitude
        for (i in samples.indices) {
            val x = samples[i]
            val a = kotlin.math.abs(x)
            if (a in 0.02f..0.55f) {
                samples[i] = (x * 1.18f).coerceIn(-1f, 1f)
            }
        }
    }
}
