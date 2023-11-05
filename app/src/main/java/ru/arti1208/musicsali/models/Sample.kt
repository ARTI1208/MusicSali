package ru.arti1208.musicsali.models

sealed class Sample(
    open val name: String,
)

data class AssetSample(
    override val name: String,
    val path: String,
) : Sample(name)

data class FileSample(
    val path: String,
) : Sample("")