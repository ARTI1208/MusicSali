package ru.arti1208.musicsali.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LayerState(
    val isPlaying: Boolean = false,
    val enabled: Boolean = true,
    val volume: Float = 1f,
    val speed: Float = 1f,
) : Parcelable
