package ru.arti1208.musicsali

import kotlinx.coroutines.flow.Flow
import ru.arti1208.musicsali.models.LayerState
import ru.arti1208.musicsali.models.Sample

interface AudioMixer {

    /**
     * Return PCM bytes
     */
    fun mixSamples(samples: List<Pair<Sample, LayerState>>): Flow<ByteArray>


}