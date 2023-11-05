package ru.arti1208.musicsali.models

data class LayerState(
    @Deprecated("dsd")
    val layer: Layer,
    val isPlaying: Boolean = false,
    val enabled: Boolean = true,
    val volume: Float = 1f,
    val speed: Float = 1f,
)
