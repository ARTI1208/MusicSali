package ru.arti1208.musicsali

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.arti1208.musicsali.models.FileSample
import ru.arti1208.musicsali.models.Layer
import ru.arti1208.musicsali.models.LayerState
import ru.arti1208.musicsali.models.ScreenState
import java.io.File
import java.io.FileOutputStream

class ScreenViewModel: ViewModel() {

    private lateinit var saliPlayer: SaliPlayer2
    private lateinit var audioMixer: AudioMixer



    private val layerStates = mutableMapOf<Layer, MutableStateFlow<LayerState>>()
    private val screenState = MutableStateFlow<ScreenState>(ScreenState(emptyList()))

    private val _layers = MutableStateFlow(emptyList<Layer>())
    val layersFlow = _layers.asStateFlow()

    val screenStateFlow = screenState.asStateFlow()

    private val _selectedLayerIndex = MutableStateFlow(-1)
    val selectedLayerIndex = _selectedLayerIndex.asStateFlow()

    private val _hasEnabledLayers = MutableStateFlow(false)
    val hasEnabledLayers = _hasEnabledLayers.asStateFlow()

    private val _selectedLayerState = MutableStateFlow<LayerState?>(null)
    val selectedLayerState = _selectedLayerState.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _intent = MutableSharedFlow<List<ByteArray>>()
    val intent = _intent.asSharedFlow()

    fun selectLayer(index: Int) {
        _selectedLayerIndex.tryEmit(index)
        _selectedLayerState.tryEmit(layerStates[_layers.value.getOrNull(index)]?.value)
    }

    private val isAnyPlaying
        get() = screenState.value.isPlaying || getPlayingLayer() != null

    private var mixedAudioTrack: AudioTrack? = null

    fun setPlayer(saliPlayer2: SaliPlayer2) {
        saliPlayer = saliPlayer2
    }

    fun setMixer(mixer: AudioMixer) {
        audioMixer = mixer
    }

    fun playPauseAll() {
        playPause(
            play = ::playAll,
            isCurrentPlay = { screenState.value.isPlaying },
        )
    }

    private fun isAnyLayerEnabled() = layerStates.values.any { it.value.enabled }

    fun pauseAll() {
        saliPlayer.stopPlaying()
    }

    private inline fun playPause(
        play: () -> Unit,
        isCurrentPlay: () -> Boolean,
    ) {
        Log.d("defeefef", "playPause")
        if (!isAnyPlaying) {
            Log.d("defeefef", "no play")
            play()
        } else {
            val wasPlaying = isCurrentPlay()
            saliPlayer.stopPlaying()

            Log.d("defeefef", "stopped: ${wasPlaying}")

            if (!wasPlaying) {
                play()
            }
        }
    }

    private fun playAll() = saliPlayer.playLayers(layerStates.map { (k, v) -> k to v.value }) {
        screenState.tryEmit(screenState.value.copy(isPlaying = it == PlayerState.PLAYING))
    }

    fun playPauseLayer(layer: Layer) {
        playPause(
            play = {
                   saliPlayer.playLayers(listOf(layer to getCurrentLayerState(layer))) {
                       val stateFlow = layerStates[layer]!!
                       stateFlow.tryEmit(stateFlow.value.copy(isPlaying = it == PlayerState.PLAYING))
                   }
            },
            isCurrentPlay = { getPlayingLayer() == layer },
        )
    }

    fun getCurrentLayerState(layer: Layer): LayerState {
        return layerStates[layer]?.value ?: DEFAULT_LAYER_STATE
    }

    private fun getPlayingLayer(): Layer? {
        return layerStates.entries.firstOrNull { it.value.value.isPlaying }?.key
    }

    fun getLayerState(layer: Layer): StateFlow<LayerState>? {
        return layerStates[layer]
    }

    fun addLayer(layer: Layer) {
        layerStates[layer] = MutableStateFlow(DEFAULT_LAYER_STATE)
        _layers.tryEmit(layerStates.keys.toList())
        _hasEnabledLayers.tryEmit(isAnyLayerEnabled())
        if (selectedLayerIndex.value < 0) {
            selectLayer(0)
        }
    }

    fun removeLayer(layer: Layer) {
        val index = _layers.value.indexOf(layer)
        layerStates.remove(layer)
        _layers.tryEmit(layerStates.keys.toList())
        _hasEnabledLayers.tryEmit(isAnyLayerEnabled())
        if (index > _layers.value.lastIndex) {
            selectLayer(index - 1)
        } else {
            selectLayer(index)
        }
    }

    fun enableLayer(layer: Layer, enabled: Boolean) {
        updateLayerState(layer, checkLayerEnabled = screenState.value.isPlaying) {
            copy(enabled = enabled)
        }

        _hasEnabledLayers.tryEmit(isAnyLayerEnabled())
    }

    fun setVolume(volume: Float) = updateSelectedLayerState {
        copy(volume = volume)
    }

    fun setSpeed(speed: Float) = updateSelectedLayerState {
        copy(speed = speed)
    }

    private inline fun updateSelectedLayerState(
        checkLayerEnabled: Boolean = false,
        transform: LayerState.() -> LayerState,
    ) {
        val layer = layersFlow.value[selectedLayerIndex.value]
        updateLayerState(layer, checkLayerEnabled, transform)
        _selectedLayerState.tryEmit(layerStates[layer]?.value)
    }

    private inline fun updateLayerState(
        layer: Layer,
        checkLayerEnabled: Boolean,
        transform: LayerState.() -> LayerState,
    ) {
        val stateFlow = layerStates[layer]!!
        stateFlow.tryEmit(stateFlow.value.transform())

        saliPlayer.updateLayers(layerStates.map { (k, v) -> k to v.value }, checkLayerEnabled)
    }

    fun recordOrShare() {
        if (mixedAudioTrack != null) {
            mixedAudioTrack?.release()
            mixedAudioTrack = null
            _isRecording.tryEmit(false)
            return
        }

        _isRecording.tryEmit(true)

        val minSze = AudioTrack.getMinBufferSize(44100, 2, AudioFormat.ENCODING_PCM_16BIT)

        val audioTrack = AudioTrack(
            AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build(),
//            result.maxOf { it.size },
            minSze,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )

        mixedAudioTrack = audioTrack

        audioTrack.play()

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = audioMixer.mixSamples(layerStates.entries.mapNotNull { (k, v) -> (k.sample to v.value).takeIf { v.value.enabled } })

                val listened = mutableListOf<ByteArray>()

                result.collect { data ->
                    runCatching {
                        audioTrack.write(data, 0, data.size)
                        listened += data
                    }.getOrElse {

                        _intent.emit(listened)

                        Log.d("ffrefref", "fnished playing")

                        runCatching {
                            audioTrack.release()
                            mixedAudioTrack = null
                        }.getOrNull()

                        throw CancellationException()
                    }
                }
            }
        }
    }

    companion object {
        private val DEFAULT_LAYER_STATE = LayerState(Layer(FileSample("")))
    }
}