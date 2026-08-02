package com.callrecorder.core.audio.encode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Encodes mono 16-bit PCM to AAC inside an MPEG-4 (.m4a) container.
 */
class AacMediaEncoder(
    private val outputPath: String,
    private val sampleRate: Int,
    private val bitrate: Int,
    private val channelCount: Int = 1,
) {
    private var codec: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var trackIndex = -1
    private var muxerStarted = false
    private var presentationTimeUs = 0L
    private val running = AtomicBoolean(false)
    private val bufferInfo = MediaCodec.BufferInfo()
    private var started = false

    fun start() {
        if (started) return
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, sampleRate * channelCount * 2)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        File(outputPath).parentFile?.mkdirs()
        val mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        codec = encoder
        muxer = mediaMuxer
        running.set(true)
        started = true
        Timber.d("AAC encoder started → $outputPath @ ${bitrate}bps / ${sampleRate}Hz")
    }

    fun encodePcm(pcm: ShortArray, sampleCount: Int) {
        if (!running.get() || !started || sampleCount <= 0) return
        val encoder = codec ?: return
        val bytes = sampleCount * 2
        val byteBuf = ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) {
            byteBuf.putShort(pcm[i])
        }
        byteBuf.flip()

        var offset = 0
        while (offset < bytes) {
            val inputIndex = encoder.dequeueInputBuffer(10_000)
            if (inputIndex >= 0) {
                val inputBuffer = encoder.getInputBuffer(inputIndex) ?: break
                inputBuffer.clear()
                val toWrite = minOf(inputBuffer.capacity(), bytes - offset)
                val slice = ByteArray(toWrite)
                byteBuf.position(offset)
                byteBuf.get(slice, 0, toWrite)
                inputBuffer.put(slice)
                val pts = presentationTimeUs
                val samplesWritten = toWrite / (2 * channelCount)
                presentationTimeUs += samplesWritten * 1_000_000L / sampleRate
                encoder.queueInputBuffer(inputIndex, 0, toWrite, pts, 0)
                offset += toWrite
                drainAvailable()
            } else {
                drainAvailable()
                break
            }
        }
    }

    fun stop(): Long {
        if (!started) {
            release()
            return 0L
        }
        running.set(false)
        val encoder = codec
        if (encoder != null) {
            try {
                val inputIndex = encoder.dequeueInputBuffer(50_000)
                if (inputIndex >= 0) {
                    encoder.queueInputBuffer(
                        inputIndex, 0, 0, presentationTimeUs,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                    )
                }
                var tries = 0
                while (tries < 100) {
                    if (drainOnce(timeoutUs = 20_000) == DrainResult.EOS) break
                    tries++
                }
            } catch (e: Exception) {
                Timber.w(e, "Encoder EOS failed")
            }
        }
        val durationMs = presentationTimeUs / 1000L
        release()
        return durationMs
    }

    fun release() {
        running.set(false)
        try { codec?.stop() } catch (_: Exception) {}
        try { codec?.release() } catch (_: Exception) {}
        codec = null
        try {
            if (muxerStarted) muxer?.stop()
        } catch (_: Exception) {}
        try { muxer?.release() } catch (_: Exception) {}
        muxer = null
        trackIndex = -1
        muxerStarted = false
        started = false
    }

    private fun drainAvailable() {
        while (drainOnce(timeoutUs = 0) == DrainResult.BUFFER) {
            // keep draining
        }
    }

    private enum class DrainResult { EMPTY, BUFFER, EOS }

    private fun drainOnce(timeoutUs: Long): DrainResult {
        val encoder = codec ?: return DrainResult.EOS
        val mediaMuxer = muxer ?: return DrainResult.EOS

        return when (val outIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)) {
            MediaCodec.INFO_TRY_AGAIN_LATER -> DrainResult.EMPTY
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                if (muxerStarted) throw IllegalStateException("Format changed twice")
                trackIndex = mediaMuxer.addTrack(encoder.outputFormat)
                mediaMuxer.start()
                muxerStarted = true
                DrainResult.BUFFER
            }
            else -> {
                if (outIndex < 0) return DrainResult.EMPTY
                val encoded = encoder.getOutputBuffer(outIndex)
                if (encoded != null && bufferInfo.size > 0 && muxerStarted) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        encoded.position(bufferInfo.offset)
                        encoded.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer.writeSampleData(trackIndex, encoded, bufferInfo)
                    }
                }
                val eos = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                encoder.releaseOutputBuffer(outIndex, false)
                if (eos) DrainResult.EOS else DrainResult.BUFFER
            }
        }
    }
}
