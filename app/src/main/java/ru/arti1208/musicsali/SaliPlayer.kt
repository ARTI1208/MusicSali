package ru.arti1208.musicsali

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.arti1208.musicsali.models.LayerState

interface SaliPlayer {

    val isPlaying: Boolean

    val progress: Double

    fun playPauseLayer(layerState: LayerState, stateFlow: MutableStateFlow<PlayerState>)

    fun playPauseLayers(layerSettings: List<LayerState>, stateFlow: MutableStateFlow<PlayerState>)

    fun getPlayingLayers(): List<LayerState>

    fun addLayer(layerState: LayerState, stateFlow: MutableStateFlow<PlayerState>)

    fun removeLayer(layerState: LayerState, stateFlow: MutableStateFlow<PlayerState>)

    fun pauseAll()

    fun pause(stateFlow: StateFlow<PlayerState>)

    fun getVisualizer(): Visualizer?
}

enum class PlayerState {
    PLAYING,
    PAUSED,
}