package ru.arti1208.musicsali

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.arti1208.musicsali.models.Layer
import ru.arti1208.musicsali.models.LayerState

interface AudioMixer {

    /**
     * Return PCM bytes
     */
    fun mixSamples(data: Map<Layer, StateFlow<LayerState>>): Flow<ByteArray>

}