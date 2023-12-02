package ru.arti1208.musicsali.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class Sample(
    open val name: String,
): Parcelable

@Parcelize
data class AssetSample(
    override val name: String,
    val path: String,
) : Sample(name)

@Parcelize
data class FileSample(
    val path: String,
) : Sample("")