package com.callrecorder.core.audio.decode

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes any MediaCodec audio file (AAC/m4a etc.) to mono 16-bit PCM shorts streaming.
 */
class PcmAudioDecoder {

    data class StreamInfo(
        val sampleRate: Int,
        val channelCount: Int,
    )

    /**
     * @param onPcm each mono short block (already mixed down if stereo)
     * @return [StreamInfo] or null if no audio track
     */
    fun decode(path: String, onPcm: (ShortArray, Int) -> Unit): StreamInfo? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(path)
            val track = selectAudioTrack(extractor) ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)

            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = decoder.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buf = decoder.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else {
                            val pts = extractor.sampleTime
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, pts, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outIndex = decoder.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    else -> {
                        if (outIndex >= 0) {
                            if (info.size > 0) {
                                val outBuf = decoder.getOutputBuffer(outIndex)!!
                                outBuf.position(info.offset)
                                outBuf.limit(info.offset + info.size)
                                val pcm = extractMonoShorts(outBuf, channels)
                                if (pcm.isNotEmpty()) {
                                    onPcm(pcm, pcm.size)
                                }
                            }
                            decoder.releaseOutputBuffer(outIndex, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                            }
                        }
                    }
                }
            }

            decoder.stop()
            decoder.release()
            return StreamInfo(sampleRate, 1)
        } catch (e: Exception) {
            Timber.e(e, "PCM decode failed for $path")
            return null
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return null
    }

    private fun extractMonoShorts(buf: ByteBuffer, channels: Int): ShortArray {
        val ordered = buf.order(ByteOrder.nativeOrder())
        val shortCount = ordered.remaining() / 2
        if (shortCount <= 0) return shortArrayOf()
        if (channels == 1) {
            val out = ShortArray(shortCount)
            ordered.asShortBuffer().get(out)
            return out
        }
        val frames = shortCount / channels
        val out = ShortArray(frames)
        for (i in 0 until frames) {
            var sum = 0
            for (c in 0 until channels) {
                sum += ordered.short.toInt()
            }
            out[i] = (sum / channels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }
}
