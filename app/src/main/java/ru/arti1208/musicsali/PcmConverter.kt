package ru.arti1208.musicsali

import android.media.MediaCodec
import android.media.MediaFormat

object PcmConverter {

    fun convert(
        data: List<ByteArray>,
        originalConfig: AudioConfig,
        targetConfig: AudioConfig,
    ): List<ByteArray> {
        val codec = MediaCodec.createEncoderByType("audio/raw")
        codec.configure(MediaFormat.createAudioFormat("audio/raw", 44100, 2), null, null, 0)

        codec.start()

        codec.stop()
        codec.release()
        TODO()
    }
}