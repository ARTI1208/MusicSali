package ru.arti1208.musicsali

import android.content.Context
import android.content.res.AssetManager
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.arti1208.musicsali.models.AssetSample
import ru.arti1208.musicsali.models.FileSample
import ru.arti1208.musicsali.models.LayerState
import ru.arti1208.musicsali.models.Sample
import kotlin.math.max


class AndroidAudioMixer(
    private val assetManager: AssetManager,
    private val context: Context,
) : AudioMixer {

    override fun mixSamples(samples: List<Pair<Sample, LayerState>>): Flow<ByteArray> {
        val samplesBytesList = samples.map { it.first.toPcmByteArray() }
        val packetIndices = IntArray(samplesBytesList.size) { 0 }

        val maxPacketCount = samplesBytesList.maxOf { it.size }

        val resultTotal = mutableListOf<ByteArray>()

        for (packetIndex in 0 until maxPacketCount) {
            val maxLength = samplesBytesList.maxOf { it.getOrNull(packetIndex)?.size ?: 0 }
            val result = ByteArray(maxLength)

            samplesBytesList.forEachIndexed { sampleIndex, samplePackets ->

                val sampleBytes = samplePackets.getOrNull(packetIndex) ?: return@forEachIndexed

                val volume = samples[sampleIndex].second.volume
                sampleBytes.forEachIndexed { index, byte ->
                    result[index] = (result[index] + (byte * volume / samples.size).toInt().toByte().also {
                        if (index > 1000 && index < 1030) {
                            Log.d(
                                "ffrefref",
                                "res: $sampleIndex / $index / $byte / $volume / ${samples.size} / $it"
                            )
                        }
                    }).toByte()
                }
            }

            resultTotal += result
        }

        return flow {
            while (true) {
                val maxLength = samplesBytesList.foldIndexed(0) { index, acc, samplePackets ->
                    val packetIndex = packetIndices[index]
                    max(samplePackets.getOrNull(packetIndex)?.size ?: 0, acc)
                }
                val result = ByteArray(maxLength)

                samplesBytesList.forEachIndexed { sampleIndex, samplePackets ->

                    val packetIndex = packetIndices[sampleIndex]

                    val sampleBytes = samplePackets.getOrNull(packetIndex) ?: return@forEachIndexed

                    val volume = samples[sampleIndex].second.volume
                    sampleBytes.forEachIndexed { index, byte ->
                        result[index] =
                            (result[index] + (byte * volume / samples.size).toInt().toByte()).toByte()
                    }

                    packetIndices[sampleIndex] = (packetIndex + 1) % samplePackets.size
                }

                Log.d("ffrefref", "yield")

                emit(result)
            }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun Sample.toPcmByteArray(): List<ByteArray> {
//        val codec: MediaCodec

        val result = mutableListOf<ByteArray>()

        val extractor = MediaExtractor()
        when (this) {
            is AssetSample -> {
                assetManager.openFd(path).use {
                    extractor.setDataSource(it)
                }
//                Log.d("ffrefref", "asset: ${extractor.getTrackFormat(0).getString(MediaFormat.KEY_MIME)}")
            }
            is FileSample -> {

                extractor.setDataSource(path)
            }
        }

        require(extractor.trackCount == 1)
        val sampleFormat = extractor.getTrackFormat(0)

        Log.d("ffrefref", "$this: ${sampleFormat.getString(MediaFormat.KEY_MIME)} / ${sampleFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)}} / ${sampleFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)}}")

        extractor.selectTrack(0)
//        val byteBuffer = ByteBuffer.allocate(extractor.sampleSize.toInt())
//        val size = extractor.readSampleData(byteBuffer, 0)
//        val bytes = ByteArray(size)
//        byteBuffer.get(bytes, 0, bytes.size)

//        Log.d("ffrefref", "recorded: extracted $size vs ${extractor.sampleSize}")

        val mediaCodec = MediaCodec.createDecoderByType(sampleFormat.getString(MediaFormat.KEY_MIME)!!)

        mediaCodec.configure(sampleFormat, null, null, 0)
        mediaCodec.start()




        extractor.selectTrack(0)

        do {

            val inputBufferIndex = mediaCodec.dequeueInputBuffer(-1)
            require(inputBufferIndex >= 0) { "ind: $inputBufferIndex" }
            val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)!!

            val size = extractor.readSampleData(inputBuffer, 0)
            if (size < 0) break

            Log.d("ffrefref", "recorded red: $size; ${extractor.sampleTrackIndex}")

            mediaCodec.queueInputBuffer(inputBufferIndex, 0, size, 0, 0)

            Log.d("ffrefref", "rquiedd")

            val bufInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufInfo, -1)

            Log.d("ffrefref", "sswddef")

            if (outputBufferIndex == -2) {
                val resFormat = mediaCodec.getOutputFormat()
                Log.d("ffrefref", "recorded red: $size; ${extractor.sampleTrackIndex}; ${resFormat.getString(MediaFormat.KEY_MIME)}")
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufInfo, -1)
            }



            require(outputBufferIndex >= 0) { "ind: $outputBufferIndex" }
            val pcm = ByteArray(bufInfo.size)
            mediaCodec.getOutputBuffer(outputBufferIndex)!!.get(pcm, 0, bufInfo.size)
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false)

            Log.d("ffrefref", "recorded red: res: ${bufInfo.size}")

            result += pcm

        } while (extractor.advance())

        mediaCodec.stop()
        mediaCodec.release()

        Log.d("ffrefref", "finished $this")

        return result
    }

    private fun MediaExtractor.toPcmByteArray(): ByteArray {
        require(trackCount == 1)
        val format = getTrackFormat(0)

        return ByteArray(0)
    }

}