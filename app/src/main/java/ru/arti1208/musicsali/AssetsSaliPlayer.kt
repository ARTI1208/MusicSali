package ru.arti1208.musicsali

import android.content.res.AssetManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.arti1208.musicsali.models.AssetSample
import ru.arti1208.musicsali.models.FileSample
import ru.arti1208.musicsali.models.Layer
import ru.arti1208.musicsali.models.LayerState

private typealias PlayerData = Pair<MediaPlayer, OnCompletionListener>

class AssetsSaliPlayer(
    private val assetManager: AssetManager,
) : SaliPlayer {

    private val mediaPlayerDataAll = mutableMapOf<StateFlow<PlayerState>, MutableMap<MediaPlayer, OnCompletionListener>>()
    private val mediaPlayerDataAll2 = mutableMapOf<DataKey, MutableList<DataValue>>()
    private val mediaPlayerDataLayer = mutableMapOf<StateFlow<PlayerState>, MutableList<Layer>>()

    override val isPlaying: Boolean
        get() = false

    override val progress: Double
        get() = 0.0

    override fun playPauseLayer(layerState: LayerState, stateFlow: MutableStateFlow<PlayerState>) {
        playPauseLayers(listOf(layerState), stateFlow)
    }

    override fun playPauseLayers(layerSettings: List<LayerState>, stateFlow: MutableStateFlow<PlayerState>) {


        if (stateFlow.value == PlayerState.PLAYING) {
            Log.d("lodkdk", "pausing: ${layerSettings.joinToString { it.layer.name }}")
            pause(listOf(stateFlow))
        } else {
            val playing = mediaPlayerDataAll.keys.filter { it.value == PlayerState.PLAYING }
            pause(playing)

            val map = mutableMapOf<MediaPlayer, OnCompletionListener>()
            layerSettings.forEach { layer ->
                playLayerAndGetData(layer, stateFlow, map)
            }
            mediaPlayerDataAll[stateFlow] = map
            mediaPlayerDataLayer[stateFlow] = layerSettings.map { it.layer }.toMutableList()
            stateFlow.tryEmit(PlayerState.PLAYING)
        }
    }

    override fun getPlayingLayers(): List<LayerState> {
        return emptyList()
//        return mediaPlayerDataAll.entries.filter { it.value.isNotEmpty() }.map { it.key }
    }

    override fun addLayer(layerState: LayerState, stateFlow: MutableStateFlow<PlayerState>) {
        if (stateFlow.value == PlayerState.PLAYING) {
            val map = mediaPlayerDataAll[stateFlow] ?: mutableMapOf()
            playLayerAndGetData(layerState, stateFlow, map)
            mediaPlayerDataAll[stateFlow] = map
        }
    }

    override fun removeLayer(layerState: LayerState, stateFlow: MutableStateFlow<PlayerState>) {
        if (stateFlow.value == PlayerState.PLAYING) {
            val map = mediaPlayerDataAll[stateFlow] ?: mutableMapOf()

        }
    }

    private fun playLayerAndGetData(
        layerState: LayerState,
        stateFlow: MutableStateFlow<PlayerState>,
        mediaPlayerData: MutableMap<MediaPlayer, OnCompletionListener>,
    ): PlayerData {
        val completionListener = createCompletionListener(mediaPlayerData, stateFlow)
        return prepareLayer(layerState).apply {
            mediaPlayerData[this] = completionListener
            setOnCompletionListener(completionListener)
            start()
            Log.d("lodkdk", "started ${layerState.layer.name}")
        } to completionListener
    }

    private fun createCompletionListener(
        mediaPlayerData: MutableMap<MediaPlayer, OnCompletionListener>,
        stateFlow: MutableStateFlow<PlayerState>,
    ) = OnCompletionListener {
        it.release()
        mediaPlayerData.remove(it)
        Log.d("lodkdk", "finished, left: ${mediaPlayerData.size}")
        if (mediaPlayerData.isEmpty()) {
            val res = stateFlow.tryEmit(PlayerState.PAUSED)
            Log.d("lodkdk", "pause: $res")
        }
    }

    private fun prepareLayer(layerState: LayerState): MediaPlayer {
        val mediaPlayer = MediaPlayer()
        when (val sample = layerState.layer.sample) {
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
            val volume = layerState.volume.coerceIn(0f, 1f)
            setVolume(volume, volume)
            playbackParams = playbackParams.apply { speed = layerState.speed }
            prepare()
        }
    }

    override fun pauseAll() {
        pause(mediaPlayerDataAll.keys.toList())
    }

    override fun pause(stateFlow: StateFlow<PlayerState>) {
        pause(listOf(stateFlow))
    }

    private fun pause(flows: List<StateFlow<PlayerState>>) {
        flows.forEach { flow ->
            val map = mediaPlayerDataAll[flow]?.toMap() ?: return@forEach
            Log.d("lodkdk", "pausing cnt: ${map.size}")
            map.forEach { (player, completionListener) ->
                if (player.isPlaying) {
                    player.pause()
                }
                completionListener.onCompletion(player)
            }
        }
    }

    override fun getVisualizer(): Visualizer? {
        return null
//        val sessionId = mediaPlayerDataAll.entries.firstOrNull()?.key?.audioSessionId ?: return null
//        return Visualizer(sessionId)
    }

    private sealed class DataKey

    private data class LayerKey(val layer: Layer): DataKey()

    private object ScreenKey : DataKey()

    private data class DataValue(
        val mediaPlayer: MediaPlayer,
        val completionListener: OnCompletionListener,
        val layer: Layer,
    )

}