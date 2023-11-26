package ru.arti1208.musicsali

import android.content.res.AssetManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.util.Log
import ru.arti1208.musicsali.models.AssetSample
import ru.arti1208.musicsali.models.FileSample
import ru.arti1208.musicsali.models.Layer
import ru.arti1208.musicsali.models.LayerState

class AndroidSaliPlayer(
    private val assetManager: AssetManager,
) : SaliPlayer {

    private val currentData = mutableMapOf<Layer, MediaPlayer>()
    private var onStateChanged: (PlayerState) -> Unit = {}

    private fun createCompletionListener(
        layer: Layer,
    ) = OnCompletionListener {
        it.release()
        currentData.remove(layer)
    }

    override fun playLayers(
        layers: List<Pair<Layer, LayerState>>,
        checkLayerEnabled: Boolean,
        onStateChanged: (PlayerState) -> Unit
    ) {
        stopPlaying()
        this.onStateChanged = onStateChanged
        layers.forEach { (layer, state) ->
            prepareLayer(layer, state, checkLayerEnabled).apply {
                currentData[layer] = this

                setOnCompletionListener(createCompletionListener(layer))
                start()
            }
        }
        Log.d("defeefef", "emit playing")
        onStateChanged(PlayerState.PLAYING)
    }

    private fun prepareLayer(layer: Layer, layerState: LayerState, checkEnabled: Boolean): MediaPlayer {
        val mediaPlayer = MediaPlayer()
        when (val sample = layer.sample) {
            is AssetSample -> {
                assetManager.openFd(sample.path).use {
                    mediaPlayer.setDataSource(it)
                }
            }
            is FileSample -> {
                mediaPlayer.setDataSource(sample.path)
            }
        }

        return mediaPlayer.apply {
            updateFromState(layerState, checkEnabled)
            isLooping = true
            prepare()
        }
    }

    override fun updateLayers(
        layers: List<Pair<Layer, LayerState>>,
        checkLayerEnabled: Boolean,
    ) {
        layers.forEach { (layer, state) ->
            currentData[layer]?.updateFromState(state, checkLayerEnabled)
        }
    }

    private fun MediaPlayer.updateFromState(state: LayerState, checkLayerEnabled: Boolean) {
        val volume = if (!checkLayerEnabled || state.enabled) state.volume.coerceIn(0f, 1f) else 0f
        setVolume(volume, volume)
        playbackParams = playbackParams.apply { speed = state.speed }
    }

    override fun stopPlaying() {
        currentData.values.forEach {
            it.release()
        }
        currentData.clear()
        Log.d("defeefef", "emit paused")
        onStateChanged(PlayerState.PAUSED)
        onStateChanged = {}
    }
}