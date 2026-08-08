package com.callrecorder.core.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Production single-pass spectral denoiser (Wiener gain + min-statistics noise track).
 *
 * Designed for telephony (narrow/wideband speech + continuous hiss / road / mic noise).
 * Overlap-add STFT with Hann window.
 *
 * Noise estimate updates only on non-speech frames (energy + spectral flatness VAD),
 * so ring/hum profile is captured without needing a separate learn pass.
 */
class WienerSpectralDenoiser(
    private val fftSize: Int = 512,
    private val hopSize: Int = 256,
    /** Minimum residual gain (prevents musical-noise holes). */
    private val gainFloor: Float = 0.08f,
    /** How aggressively noise estimate follows non-speech. */
    private val noiseAdapt: Float = 0.15f,
) {
    private val real = FloatArray(fftSize)
    private val imag = FloatArray(fftSize)
    private val window = FloatArray(fftSize) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
    }
    /** Smoothed noise magnitude per bin. */
    private val noiseMag = FloatArray(fftSize / 2 + 1) { 1e-4f }
    private val smoothMag = FloatArray(fftSize / 2 + 1)
    private val inputFifo = FloatArray(fftSize)
    private val outputFifo = FloatArray(fftSize)
    private var fifoFill = 0
    private var frames = 0
    private var noiseFloorRms = 0.01f

    fun reset() {
        noiseMag.fill(1e-4f)
        smoothMag.fill(0f)
        inputFifo.fill(0f)
        outputFifo.fill(0f)
        fifoFill = 0
        frames = 0
        noiseFloorRms = 0.01f
    }

    fun process(input: FloatArray): FloatArray {
        val out = ArrayList<Float>(input.size + hopSize)
        var idx = 0
        while (idx < input.size) {
            val need = fftSize - fifoFill
            val take = min(need, input.size - idx)
            System.arraycopy(input, idx, inputFifo, fifoFill, take)
            fifoFill += take
            idx += take
            if (fifoFill < fftSize) break

            for (i in 0 until fftSize) {
                real[i] = inputFifo[i] * window[i]
                imag[i] = 0f
            }
            Fft.transform(real, imag, inverse = false)

            val half = fftSize / 2
            var frameEnergy = 0.0
            for (k in 0..half) {
                val mag = sqrt(real[k] * real[k] + imag[k] * imag[k])
                smoothMag[k] = if (frames == 0) mag else 0.7f * smoothMag[k] + 0.3f * mag
                frameEnergy += (mag * mag).toDouble()
            }
            val frameRms = sqrt(frameEnergy / (half + 1)).toFloat()

            // Spectral flatness (tonality): high flatness ≈ noise / hiss; low ≈ formants/speech
            var logSum = 0.0
            var linSum = 0.0
            val bins = half + 1
            for (k in 1 until bins) {
                val m = max(smoothMag[k], 1e-8f).toDouble()
                logSum += ln(m)
                linSum += m
            }
            val geo = kotlin.math.exp(logSum / (bins - 1))
            val ari = linSum / (bins - 1)
            val flatness = (geo / max(ari, 1e-12)).toFloat().coerceIn(0f, 1f)

            val speechLike =
                frameRms > noiseFloorRms * 2.2f && flatness < 0.55f

            if (!speechLike || frames < 8) {
                // Adapt noise profile on silence / hiss / ring tone tails
                val a = if (frames < 20) 0.35f else noiseAdapt
                for (k in 0..half) {
                    noiseMag[k] = (1f - a) * noiseMag[k] + a * smoothMag[k]
                }
                noiseFloorRms = 0.95f * noiseFloorRms + 0.05f * frameRms
            }

            // Wiener spectral gain
            for (k in 0..half) {
                val re = real[k]
                val im = imag[k]
                val mag = max(sqrt(re * re + im * im), 1e-12f)
                val n = max(noiseMag[k], 1e-12f)
                // Prior SNR estimate
                val snr = max((mag * mag) / (n * n) - 1f, 0f)
                // Wiener gain + gentle over-suppression at low SNR
                var gain = snr / (snr + 1f)
                if (snr < 1.5f) {
                    gain *= 0.75f // extra hush on noise-like bins
                }
                gain = max(gain, gainFloor)
                // Soft spectral floor: never collapse total silence into tonal artifacts
                val cleaned = max(mag * gain, gainFloor * n * 0.5f)
                val scale = cleaned / mag
                real[k] = re * scale
                imag[k] = im * scale
                if (k > 0 && k < half) {
                    real[fftSize - k] = real[k]
                    imag[fftSize - k] = -imag[k]
                }
            }

            Fft.transform(real, imag, inverse = true)

            for (i in 0 until fftSize) {
                outputFifo[i] += real[i] * window[i]
            }
            for (i in 0 until hopSize) {
                out.add(outputFifo[i].coerceIn(-1f, 1f))
            }
            System.arraycopy(outputFifo, hopSize, outputFifo, 0, fftSize - hopSize)
            for (i in fftSize - hopSize until fftSize) outputFifo[i] = 0f
            System.arraycopy(inputFifo, hopSize, inputFifo, 0, fftSize - hopSize)
            fifoFill = fftSize - hopSize
            frames++
        }
        return out.toFloatArray()
    }

    /** Flush remaining STFT samples (call once at end of stream). */
    fun flush(): FloatArray {
        if (fifoFill == 0) return floatArrayOf()
        val pad = FloatArray(fftSize - fifoFill)
        return process(pad)
    }
}
