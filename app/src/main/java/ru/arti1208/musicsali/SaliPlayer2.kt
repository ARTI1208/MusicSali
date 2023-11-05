package ru.arti1208.musicsali

import ru.arti1208.musicsali.models.Layer
import ru.arti1208.musicsali.models.LayerState

interface SaliPlayer2 {

    fun playLayers(
        layers: List<Pair<Layer, LayerState>>,
        checkLayerEnabled: Boolean = true,
        onStateChanged: (PlayerState) -> Unit,
    )

    fun updateLayers(
        layers: List<Pair<Layer, LayerState>>,
        checkLayerEnabled: Boolean = true,
    )

    fun stopPlaying()


}