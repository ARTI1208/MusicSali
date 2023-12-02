package ru.arti1208.musicsali

import android.content.Context
import android.content.res.AssetManager
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import ru.arti1208.musicsali.models.AssetSample
import ru.arti1208.musicsali.models.FileSample
import ru.arti1208.musicsali.models.Layer
import ru.arti1208.musicsali.models.LayerState
import ru.arti1208.musicsali.models.Sample


class AndroidAudioMixer(
    private val assetManager: AssetManager,
    private val context: Context,
) : AudioMixer {

    override fun mixSamples(data: Map<Layer, StateFlow<LayerState>>): Flow<ByteArray> {
        val states = data.values.toList()
        val samplesBytesList = data.map { it.key.sample.toPcmByteArray() }

        val packetIndices = IntArray(samplesBytesList.size) { 0 }
        val bufferSize = samplesBytesList.maxOf { it.maxOf { it.size } }

        return flow {
            val result = ByteArray(bufferSize)
            while (true) {
//                val maxLength = samplesBytesList.foldIndexed(0) { index, acc, samplePackets ->
//                    val packetIndex = packetIndices[index]
//                    max(samplePackets.getOrNull(packetIndex)?.size ?: 0, acc)
//                }


//                val enabledCount = states.count { it.value.enabled }
                val layerCount = data.size

                var anyWritten = false

                samplesBytesList.forEachIndexed { sampleIndex, samplePackets ->
                    if (!states[sampleIndex].value.enabled) return@forEachIndexed

                    val packetIndex = packetIndices[sampleIndex]

                    val sampleBytes = samplePackets.getOrNull(packetIndex) ?: return@forEachIndexed

                    val volume = states[sampleIndex].value.volume
                    val koef = volume / layerCount
                    for (index in 0 until bufferSize) {
                        val previous = if (anyWritten) result[index] else 0
                        val sampleValue = if (layerCount == 0) 0 else sampleBytes.getOrNull(index)?.let { it * koef }?.toInt() ?: 0
                        result[index] = (previous + sampleValue).toByte()
                    }
                    anyWritten = true

                    packetIndices[sampleIndex] = (packetIndex + 1) % samplePackets.size
                }

                emit(result)
            }
        }
    }

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

        val sampleRate = sampleFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = sampleFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val format = sampleFormat.getString(MediaFormat.KEY_MIME)

        Log.d("ffrefref", "gotData: $this: ${sampleFormat.getString(MediaFormat.KEY_MIME)} / ${sampleFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)}} / ${sampleFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)}}")

        extractor.selectTrack(0)
//        val byteBuffer = ByteBuffer.allocate(extractor.sampleSize.toInt())
//        val size = extractor.readSampleData(byteBuffer, 0)
//        val bytes = ByteArray(size)
//        byteBuffer.get(bytes, 0, bytes.size)

//        Log.d("ffrefref", "recorded: extracted $size vs ${extractor.sampleSize}")

        val mediaCodec = MediaCodec.createDecoderByType(sampleFormat.getString(MediaFormat.KEY_MIME)!!)

        mediaCodec.configure(sampleFormat, null, null, 0)
        mediaCodec.start()

        val targetAudioFormat = MediaFormat.createAudioFormat("audio/raw", 44100, 2)
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.also {
            Log.d("fjijfrif", "codecs: ${it.joinToString("|") { "${it.name} , ${it.supportedTypes.joinToString()}, ${it.isEncoder}" }}")
        }

        val encoder = when (sampleRate != 44100 || channelCount != 2) {
//            true -> MediaCodec.createEncoderByType("audio/raw")
            true -> null as? MediaCodec
            false -> null
        }?.apply {
            val targetAudioFormat = MediaFormat.createAudioFormat("audio/raw", 44100, 2)
            configure(targetAudioFormat, null, null, 0)
            start()
        }

        fun transform(data: ByteArray): ByteArray {
            encoder ?: return data

            val encoderInpBufInd = encoder.dequeueInputBuffer(-1)
            require(encoderInpBufInd >= 0) { "indEnc: $encoderInpBufInd" }

            encoder.queueInputBuffer(encoderInpBufInd, 0, data.size, 0, 0)

            val bufInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = encoder.dequeueOutputBuffer(bufInfo, -1)

            require(outputBufferIndex >= 0) { "indEnc: $outputBufferIndex" }

            val pcmTrans = ByteArray(bufInfo.size)
            mediaCodec.getOutputBuffer(outputBufferIndex)!!.get(pcmTrans, 0, bufInfo.size)
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false)

            return pcmTrans
        }

        extractor.selectTrack(0)

        do {

            val inputBufferIndex = mediaCodec.dequeueInputBuffer(-1)
            require(inputBufferIndex >= 0) { "ind: $inputBufferIndex" }
            val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)!!

            val size = extractor.readSampleData(inputBuffer, 0)
            if (size < 0) break

//            Log.d("ffrefref", "recorded red: $size; ${extractor.sampleTrackIndex}")

            mediaCodec.queueInputBuffer(inputBufferIndex, 0, size, 0, 0)

//            Log.d("ffrefref", "rquiedd")

            val bufInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufInfo, -1)

//            Log.d("ffrefref", "sswddef")

            if (outputBufferIndex == -2) {
                val resFormat = mediaCodec.outputFormat
//                Log.d("ffrefref", "recorded red: $size; ${extractor.sampleTrackIndex}; ${resFormat.getString(MediaFormat.KEY_MIME)}")
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufInfo, -1)
            }



            require(outputBufferIndex >= 0) { "ind: $outputBufferIndex" }
            val pcm = ByteArray(bufInfo.size)
            mediaCodec.getOutputBuffer(outputBufferIndex)!!.get(pcm, 0, bufInfo.size)
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false)

//            Log.d("ffrefref", "recorded red: res: ${bufInfo.size}")

            result += transform(pcm)

        } while (extractor.advance())

        mediaCodec.stop()
        mediaCodec.release()

        encoder?.apply {
            stop()
            release()
        }

        Log.d("ffrefref", "finished $this")

        return result
    }

    private fun MediaExtractor.toPcmByteArray(): ByteArray {
        require(trackCount == 1)
        val format = getTrackFormat(0)

        return ByteArray(0)
    }

}