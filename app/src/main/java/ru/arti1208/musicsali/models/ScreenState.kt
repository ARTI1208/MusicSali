package ru.arti1208.musicsali.models

data class ScreenState(
    val layers: List<LayerState>,
    val isPlaying: Boolean = false,
    val progress: Double = 0.0,
)