package com.callrecorder.core.audio.dsp

/**
 * In-place radix-2 Cooley–Tukey FFT for real-time spectral processing.
 * [real] / [imag] length must be a power of two.
 */
internal object Fft {
    fun transform(real: FloatArray, imag: FloatArray, inverse: Boolean = false) {
        val n = real.size
        require(n == imag.size && n > 0 && n and (n - 1) == 0) {
            "FFT size must be power of two"
        }

        // Bit-reverse permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }

        var len = 2
        while (len <= n) {
            val ang = (if (inverse) 2.0 else -2.0) * Math.PI / len
            val wlenCos = kotlin.math.cos(ang).toFloat()
            val wlenSin = kotlin.math.sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var wReal = 1f
                var wImag = 0f
                for (k in 0 until len / 2) {
                    val uReal = real[i + k]
                    val uImag = imag[i + k]
                    val vReal = real[i + k + len / 2] * wReal - imag[i + k + len / 2] * wImag
                    val vImag = real[i + k + len / 2] * wImag + imag[i + k + len / 2] * wReal
                    real[i + k] = uReal + vReal
                    imag[i + k] = uImag + vImag
                    real[i + k + len / 2] = uReal - vReal
                    imag[i + k + len / 2] = uImag - vImag
                    val nextWReal = wReal * wlenCos - wImag * wlenSin
                    wImag = wReal * wlenSin + wImag * wlenCos
                    wReal = nextWReal
                }
                i += len
            }
            len = len shl 1
        }

        if (inverse) {
            val inv = 1f / n
            for (i in 0 until n) {
                real[i] *= inv
                imag[i] *= inv
            }
        }
    }
}
