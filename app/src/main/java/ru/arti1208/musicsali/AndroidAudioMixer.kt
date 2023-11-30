package ru.arti1208.musicsali

import android.content.Context
import android.content.res.AssetManager
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.arti1208.musicsali.models.AssetSample
import ru.arti1208.musicsali.models.FileSample
import ru.arti1208.musicsali.models.Layer
import ru.arti1208.musicsali.models.LayerState
import ru.arti1208.musicsali.models.Sample
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class AndroidAudioMixer(
    private val assetManager: AssetManager,
    private val context: Context,
) : AudioMixer {

    override suspend fun mixSamples(data: Map<Layer, StateFlow<LayerState>>): Flow<ByteArray> {
        val states = data.values.toList()
//        val samplesBytesList = data.map { it.key.sample.toPcmByteArray() }

        val targetPaths = data.map { unify(it.key.sample) }
        val samplesBytesList = targetPaths.map {
            val extractor = MediaExtractor().apply {
                setDataSource(it)
            }
            toPcmByteArray(extractor)
        }

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

    @OptIn(UnstableApi::class)
    private suspend fun unify(sample: Sample) = suspendCoroutine<String> { continuation ->

            val mediaItem = when (sample) {
                is FileSample -> File(sample.path).toUri()
                is AssetSample -> "file:///android_asset/${sample.path}".toUri()
            }.let { MediaItem.fromUri(it) }

            val tmp = File.createTempFile("sampleConvert", null)
            val tmpPath = tmp.path

            val edited = EditedMediaItem.Builder(mediaItem)
                .setEffects(
                    Effects(
                        listOf(
                            SonicAudioProcessor().apply {
//                            setSpeed(layerState.speed)
//                            setVolume(layerState.volume)
                                setOutputSampleRateHz(44100)
                                configure(
                                    AudioProcessor.AudioFormat(
                                        44100, 2, C.ENCODING_PCM_16BIT,
                                    ))
                            }
                        ),
                        emptyList(),
                    )
                ).build()

            runBlocking(Dispatchers.Main) {
                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(
                            composition: Composition,
                            exportResult: ExportResult
                        ) {
                            Log.d("hhhhh", "completed: ${exportResult}")
                            continuation.resume(tmpPath)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            Log.d("hhhhh", "errror", exportException)
                            continuation.resumeWithException(exportException)
                        }
                    })
                    .build()
                    .also {
                        Log.d("rgrgrtgrg", "l: ${Looper.myLooper()?.thread}")
                    }
                    .start(edited, tmpPath)
            }
    }

    private fun Sample.toPcmByteArray(): List<ByteArray> {

        val extractor = MediaExtractor()
        when (this) {
            is AssetSample -> {
                assetManager.openFd(path).use {
                    extractor.setDataSource(it)
                }
            }
            is FileSample -> {
                extractor.setDataSource(path)
            }
        }

        return toPcmByteArray(extractor)
    }

    private fun toPcmByteArray(extractor: MediaExtractor): List<ByteArray> {

        val result = mutableListOf<ByteArray>()

        require(extractor.trackCount == 1)
        val sampleFormat = extractor.getTrackFormat(0)

        val sampleRate = sampleFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = sampleFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val format = sampleFormat.getString(MediaFormat.KEY_MIME)

        Log.d("ffrefref", "gotData: $this: ${sampleFormat.getString(MediaFormat.KEY_MIME)} / ${sampleFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)}} / ${sampleFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)}}")

        extractor.selectTrack(0)

        val mediaCodec = MediaCodec.createDecoderByType(sampleFormat.getString(MediaFormat.KEY_MIME)!!)

        mediaCodec.configure(sampleFormat, null, null, 0)
        mediaCodec.start()

//        val targetAudioFormat = MediaFormat.createAudioFormat("audio/raw", 44100, 2)
//        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.also {
//            Log.d("fjijfrif", "codecs: ${it.joinToString("|") { "${it.name} , ${it.supportedTypes.joinToString()}, ${it.isEncoder}" }}")
//        }

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

            Log.d("ffrefref", "recorded red: $size; ${extractor.sampleTrackIndex}")

            mediaCodec.queueInputBuffer(inputBufferIndex, 0, size, 0, 0)

            Log.d("ffrefref", "rquiedd")

            val bufInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufInfo, -1)

            Log.d("ffrefref", "sswddef")

            if (outputBufferIndex == -2) {
                val resFormat = mediaCodec.outputFormat
                Log.d("ffrefref", "recorded red: $size; ${extractor.sampleTrackIndex}; ${resFormat.getString(MediaFormat.KEY_MIME)}")
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufInfo, -1)
            }



            require(outputBufferIndex >= 0) { "ind: $outputBufferIndex" }
            val pcm = ByteArray(bufInfo.size)
            mediaCodec.getOutputBuffer(outputBufferIndex)!!.get(pcm, 0, bufInfo.size)
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false)

            Log.d("ffrefref", "recorded red: res: ${bufInfo.size}")

            result += transform(pcm)

        } while (extractor.advance())

        mediaCodec.stop()
        mediaCodec.release()

        encoder?.apply {
            stop()
            release()
        }

        Log.d("ffrefref", "finished $this, sz: ${result.sumOf { it.size }}")

        return result
    }

}