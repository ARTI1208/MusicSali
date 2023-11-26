package ru.arti1208.musicsali

import java.io.File
import java.io.FileOutputStream

interface PcmDataWriter {

    fun FileOutputStream.writeData(data: String) {
        data.forEach { write(it.code) }
    }

    fun FileOutputStream.writeData(data: Int) {
        write(data)
        write(data shr 8)
        write(data shr 16)
        write(data shr 24)
    }

    fun FileOutputStream.writeData(data: Short) {
        val asInt = data.toInt()
        write(asInt)
        write(asInt shr 8)
    }

    fun FileOutputStream.writePcmData(
        data: List<ByteArray>,
        channelCount: Int,
        sampleRate: Int,
        bitsPerSample: Int,
    )

    fun FileOutputStream.writePcmData(
        data: File,
        channelCount: Int,
        sampleRate: Int,
        bitsPerSample: Int,
    )
}