package com.callrecorder.core.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Overlap-add spectral subtraction noise reducer.
 *
 * Learn phase: average magnitude spectrum of ambient / ringback noise.
 * Process phase: subtract noise profile with over-subtraction factor, keep residual floor.
 */
class SpectralNoiseSubtractor(
    private val fftSize: Int = 512,
    private val hopSize: Int = 256,
    private val overSubtraction: Float = 1.8f,
    private val spectralFloor: Float = 0.05f,
) {
    private val real = FloatArray(fftSize)
    private val imag = FloatArray(fftSize)
    private val window = FloatArray(fftSize) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat() // Hann
    }
    private val noiseMag = FloatArray(fftSize / 2 + 1)
    private val inputFifo = FloatArray(fftSize)
    private val outputFifo = FloatArray(fftSize)
    private var fifoFill = 0
    private var noiseFrames = 0
    private var learning = true

    val isLearning: Boolean get() = learning
    val noiseFramesLearned: Int get() = noiseFrames

    fun freezeNoiseProfile() {
        learning = false
    }

    fun resumeLearning() {
        learning = true
    }

    fun reset() {
        noiseMag.fill(0f)
        inputFifo.fill(0f)
        outputFifo.fill(0f)
        fifoFill = 0
        noiseFrames = 0
        learning = true
    }

    /**
     * Process a block of mono PCM floats in [-1, 1].
     * Returns processed samples (may be shorter than input while FIFO warms up).
     */
    fun process(input: FloatArray): FloatArray {
        val out = ArrayList<Float>(input.size)
        var idx = 0
        while (idx < input.size) {
            val need = fftSize - fifoFill
            val take = minOf(need, input.size - idx)
            System.arraycopy(input, idx, inputFifo, fifoFill, take)
            fifoFill += take
            idx += take

            if (fifoFill < fftSize) break

            // Windowed FFT
            for (i in 0 until fftSize) {
                real[i] = inputFifo[i] * window[i]
                imag[i] = 0f
            }
            Fft.transform(real, imag, inverse = false)

            val half = fftSize / 2
            if (learning) {
                for (k in 0..half) {
                    val mag = sqrt(real[k] * real[k] + imag[k] * imag[k])
                    noiseMag[k] += mag
                }
                noiseFrames++
            } else if (noiseFrames > 0) {
                for (k in 0..half) {
                    val re = real[k]
                    val im = imag[k]
                    val mag = sqrt(re * re + im * im)
                    val n = noiseMag[k] / noiseFrames
                    val cleaned = (mag - overSubtraction * n).coerceAtLeast(spectralFloor * mag)
                    val scale = if (mag > 1e-8f) cleaned / mag else 0f
                    real[k] = re * scale
                    imag[k] = im * scale
                    if (k > 0 && k < half) {
                        real[fftSize - k] = real[k]
                        imag[fftSize - k] = -imag[k]
                    }
                }
            }

            Fft.transform(real, imag, inverse = true)

            // Overlap-add
            for (i in 0 until fftSize) {
                outputFifo[i] += real[i] * window[i]
            }
            for (i in 0 until hopSize) {
                out.add(outputFifo[i].coerceIn(-1f, 1f))
            }
            // Shift FIFO
            System.arraycopy(outputFifo, hopSize, outputFifo, 0, fftSize - hopSize)
            for (i in fftSize - hopSize until fftSize) outputFifo[i] = 0f
            System.arraycopy(inputFifo, hopSize, inputFifo, 0, fftSize - hopSize)
            fifoFill = fftSize - hopSize
        }
        return out.toFloatArray()
    }
}
