package ru.arti1208.musicsali

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

object WavPcmWriter : PcmDataWriter {

    override fun FileOutputStream.writePcmData(
        data: List<ByteArray>,
        channelCount: Int,
        sampleRate: Int,
        bitsPerSample: Int
    ) {
        val inputSize = data.sumOf { it.size }
        writeData("RIFF")
        writeData(36 + inputSize)
        writeData("WAVE")
        writeData("fmt ")
        writeData(16)
        writeData(1.toShort())
        writeData(channelCount.toShort())
        writeData(sampleRate)
        writeData(sampleRate * channelCount * bitsPerSample / 8)
        writeData((channelCount * bitsPerSample / 8).toShort())
        writeData(bitsPerSample.toShort())
        writeData("data")
        writeData(inputSize)

        data.forEach { bytes ->
            write(bytes)
        }
    }

    override fun FileOutputStream.writePcmData(
        data: File,
        channelCount: Int,
        sampleRate: Int,
        bitsPerSample: Int
    ) {
        val inputSize = data.length().toInt()
        writeData("RIFF")
        writeData(36 + inputSize)
        writeData("WAVE")
        writeData("fmt ")
        writeData(16)
        writeData(1.toShort())
        writeData(channelCount.toShort())
        writeData(sampleRate)
        writeData(sampleRate * channelCount * bitsPerSample / 8)
        writeData((channelCount * bitsPerSample / 8).toShort())
        writeData(bitsPerSample.toShort())
        writeData("data")
        writeData(inputSize)

        Files.copy(data.toPath(), this)
    }

}