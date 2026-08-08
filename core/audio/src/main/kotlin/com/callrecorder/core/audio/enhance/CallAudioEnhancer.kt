package com.callrecorder.core.audio.enhance

import com.callrecorder.core.audio.decode.PcmAudioDecoder
import com.callrecorder.core.audio.dsp.LoudnessNormalizer
import com.callrecorder.core.audio.dsp.NoiseReductionPipeline
import com.callrecorder.core.audio.dsp.ProductionSpeechChain
import com.callrecorder.core.audio.encode.AacMediaEncoder
import com.callrecorder.core.domain.model.AudioQuality
import timber.log.Timber
import java.io.File
import kotlin.math.abs

/**
 * Production post-processor for finished call captures.
 *
 * Live telephony capture usually uses [android.media.MediaRecorder] (no mid-call DSP).
 * After hangup this class:
 * 1. Decodes AAC → mono PCM (stream)
 * 2. Runs [ProductionSpeechChain] — Wiener spectral denoise + gate + compressor
 * 3. Trims leading ringback / dead air (bounded)
 * 4. Re-encodes HD AAC and atomically replaces the original file
 *
 * On any failure the original capture is kept untouched.
 */
class CallAudioEnhancer {

    data class Result(
        val durationMs: Long,
        val outputPath: String,
        val enhanced: Boolean,
    )

    /**
     * Enhance [inputPath] and replace it in place when successful.
     */
    fun enhanceInPlace(inputPath: String, quality: AudioQuality): Result {
        val input = File(inputPath)
        if (!input.exists() || input.length() < 256L) {
            return Result(0L, inputPath, enhanced = false)
        }

        val tempOut = File(input.parentFile, "${input.nameWithoutExtension}_hd.tmp.m4a")
        if (tempOut.exists()) tempOut.delete()

        return try {
            val processed = processTo(inputPath, tempOut.absolutePath, quality)
            if (!processed.enhanced || !tempOut.exists() || tempOut.length() < 256L) {
                tempOut.delete()
                return Result(0L, inputPath, enhanced = false)
            }

            val backup = File(input.parentFile, "${input.name}.bak")
            if (backup.exists()) backup.delete()

            val backedUp = input.renameTo(backup)
            if (!backedUp) {
                Timber.w("Backup rename failed — leave original, drop enhance")
                tempOut.delete()
                return Result(0L, inputPath, enhanced = false)
            }

            if (tempOut.renameTo(input)) {
                backup.delete()
                Timber.i("HD denoise replace OK → $inputPath")
                Result(processed.durationMs, inputPath, enhanced = true)
            } else {
                // Restore original
                input.delete()
                backup.renameTo(input)
                tempOut.delete()
                Timber.e("Replace failed — original restored")
                Result(0L, inputPath, enhanced = false)
            }
        } catch (e: Exception) {
            Timber.e(e, "enhanceInPlace failed — original kept")
            tempOut.delete()
            Result(0L, inputPath, enhanced = false)
        }
    }

    fun processTo(
        inputPath: String,
        outputPath: String,
        quality: AudioQuality,
    ): Result {
        val sampleRate = probeSampleRate(inputPath)
            ?: return Result(0L, inputPath, enhanced = false)

        val chain = ProductionSpeechChain(sampleRate).also { it.reset() }
        val bitrate = quality.bitrate.coerceIn(96_000, 320_000)
        val encoder = AacMediaEncoder(outputPath, sampleRate, bitrate)
        encoder.start()

        val maxLeadSkip = (sampleRate * MAX_LEAD_TRIM_SEC).toInt()
        val speechHoldSamples = (sampleRate * SPEECH_HOLD_SEC).toInt().coerceAtLeast(1)
        var leadSkipped = 0
        var speechRun = 0
        var armed = false
        var writtenSamples = 0L

        val floatBuf = FloatArray(CHUNK)
        val shortOut = ShortArray(CHUNK)
        val loudness = LoudnessNormalizer.Streaming(targetRms = 0.18f, maxGain = 10f)

        val stream = PcmAudioDecoder().decode(inputPath) { pcm, count ->
            var offset = 0
            while (offset < count) {
                val n = minOf(CHUNK, count - offset)
                for (i in 0 until n) {
                    floatBuf[i] = pcm[offset + i] / 32768f
                }
                offset += n

                val frame = floatBuf.copyOf(n)
                val cleaned = chain.process(frame)
                loudness.process(cleaned)

                val toWrite = if (!armed) {
                    armAndSlice(
                        cleaned = cleaned,
                        leadSkipped = leadSkipped,
                        speechRun = speechRun,
                        maxLeadSkip = maxLeadSkip,
                        speechHoldSamples = speechHoldSamples,
                    ).also { state ->
                        leadSkipped = state.leadSkipped
                        speechRun = state.speechRun
                        armed = state.armed
                    }.pcm
                } else {
                    cleaned
                }

                if (toWrite == null || toWrite.isEmpty()) continue

                val written = NoiseReductionPipeline.floatToShorts(toWrite, shortOut)
                encoder.encodePcm(shortOut, written)
                writtenSamples += written
            }
        }

        if (stream == null) {
            encoder.release()
            File(outputPath).delete()
            Timber.e("Decode failed during enhance")
            return Result(0L, inputPath, enhanced = false)
        }

        val tail = chain.flush()
        if (tail.isNotEmpty() && armed) {
            val written = NoiseReductionPipeline.floatToShorts(tail, shortOut)
            encoder.encodePcm(shortOut, written)
            writtenSamples += written
        }

        // Quiet entire file never tripped lead-arm → re-run denoise with no trim
        if (!armed || writtenSamples < sampleRate / 5) {
            encoder.release()
            File(outputPath).delete()
            Timber.w("Lead-trim path empty — retry denoise without trim")
            return processNoTrim(inputPath, outputPath, quality, sampleRate)
        }

        val durationMs = encoder.stop().let { encDur ->
            if (encDur > 0) encDur else writtenSamples * 1000L / sampleRate
        }

        Timber.i(
            "Enhance stream OK sr=$sampleRate samples=$writtenSamples " +
                "leadSkip=$leadSkipped dur=${durationMs}ms br=$bitrate"
        )
        return Result(durationMs, outputPath, enhanced = true)
    }

    /** Full denoise encode, no leading-silence removal. */
    private fun processNoTrim(
        inputPath: String,
        outputPath: String,
        quality: AudioQuality,
        sampleRate: Int,
    ): Result {
        val chain = ProductionSpeechChain(sampleRate).also { it.reset() }
        val bitrate = quality.bitrate.coerceIn(96_000, 320_000)
        val encoder = AacMediaEncoder(outputPath, sampleRate, bitrate)
        encoder.start()
        var writtenSamples = 0L
        val floatBuf = FloatArray(CHUNK)
        val shortOut = ShortArray(CHUNK)
        val loudness = LoudnessNormalizer.Streaming(targetRms = 0.18f, maxGain = 10f)

        val stream = PcmAudioDecoder().decode(inputPath) { pcm, count ->
            var offset = 0
            while (offset < count) {
                val n = minOf(CHUNK, count - offset)
                for (i in 0 until n) {
                    floatBuf[i] = pcm[offset + i] / 32768f
                }
                offset += n
                val cleaned = chain.process(floatBuf.copyOf(n))
                loudness.process(cleaned)
                val written = NoiseReductionPipeline.floatToShorts(cleaned, shortOut)
                encoder.encodePcm(shortOut, written)
                writtenSamples += written
            }
        }

        if (stream == null) {
            encoder.release()
            File(outputPath).delete()
            return Result(0L, inputPath, enhanced = false)
        }

        val tail = chain.flush()
        if (tail.isNotEmpty()) {
            val written = NoiseReductionPipeline.floatToShorts(tail, shortOut)
            encoder.encodePcm(shortOut, written)
            writtenSamples += written
        }

        if (writtenSamples < sampleRate / 5) {
            encoder.release()
            File(outputPath).delete()
            return Result(0L, inputPath, enhanced = false)
        }

        val durationMs = encoder.stop().let { encDur ->
            if (encDur > 0) encDur else writtenSamples * 1000L / sampleRate
        }
        Timber.i("Enhance no-trim OK samples=$writtenSamples dur=${durationMs}ms")
        return Result(durationMs, outputPath, enhanced = true)
    }

    private data class ArmState(
        val pcm: FloatArray?,
        val leadSkipped: Int,
        val speechRun: Int,
        val armed: Boolean,
    )

    private fun armAndSlice(
        cleaned: FloatArray,
        leadSkipped: Int,
        speechRun: Int,
        maxLeadSkip: Int,
        speechHoldSamples: Int,
    ): ArmState {
        var skip = leadSkipped
        var run = speechRun
        var i = 0
        while (i < cleaned.size) {
            if (skip >= maxLeadSkip) {
                // Budget spent — keep rest of this frame
                val rest = cleaned.copyOfRange(i, cleaned.size)
                return ArmState(rest, skip, run, armed = true)
            }
            if (abs(cleaned[i]) > SPEECH_ARM_LEVEL) {
                run++
                if (run >= speechHoldSamples) {
                    val start = maxOf(0, i - speechHoldSamples / 4)
                    return ArmState(
                        pcm = cleaned.copyOfRange(start, cleaned.size),
                        leadSkipped = skip + start,
                        speechRun = run,
                        armed = true,
                    )
                }
            } else {
                run = maxOf(0, run - 2)
            }
            i++
            skip++
        }
        // Entire chunk still lead silence
        return ArmState(pcm = null, leadSkipped = skip, speechRun = run, armed = false)
    }

    private fun probeSampleRate(path: String): Int? {
        val ex = android.media.MediaExtractor()
        return try {
            ex.setDataSource(path)
            for (i in 0 until ex.trackCount) {
                val fmt = ex.getTrackFormat(i)
                val mime = fmt.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    return fmt.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE)
                }
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "probeSampleRate failed")
            null
        } finally {
            try {
                ex.release()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val CHUNK = 4096
        private const val MAX_LEAD_TRIM_SEC = 8f
        private const val SPEECH_HOLD_SEC = 0.08f
        private const val SPEECH_ARM_LEVEL = 0.018f
    }
}
