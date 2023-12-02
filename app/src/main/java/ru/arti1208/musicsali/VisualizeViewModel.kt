package ru.arti1208.musicsali

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.arti1208.musicsali.models.Layer
import ru.arti1208.musicsali.models.LayerState
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random

class VisualizeViewModel(
    private val saliPlayer: SaliPlayer,
    private val audioMixer: AudioMixer,
    private val data: Map<Layer, LayerState>,
) : ViewModel() {

    private val randomSeed = data.size + data.values.sumOf { it.speed.toDouble() + it.volume.toDouble() }.toInt()
    val random = Random(randomSeed)

    private val _currentTime = MutableStateFlow(0L)
    val currentTime = _currentTime.asStateFlow()
    val currentTimeStr = _currentTime.map {
        timeToString(it)
    }

    private val _totalTime = MutableStateFlow(0L)
    val totalTime = _totalTime.asStateFlow()
    val totalTimeStr = _totalTime.map {
        timeToString(it)
    }

    private val _state = MutableStateFlow(State.RECORDING)
    val state = _state.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private var animateJob: Job? = null

    private val ids = listOf(
        R.drawable.ellipse,
        R.drawable.circle,
        R.drawable.dots,
        R.drawable.lines1,
        R.drawable.lines2,
        R.drawable.lines3,
        R.drawable.polygon1,
        R.drawable.polygon2,
        R.drawable.polygon3,
        R.drawable.spiral1,
        R.drawable.spiral2,
        R.drawable.spiral3,
        R.drawable.star1,
        R.drawable.star2,
        R.drawable.star3,
    )

    private val _images = MutableStateFlow(generateRandomImages())
    val images = _images.asStateFlow()

    private fun timeToString(time: Long): String {
        val minutes = time / 60
        val seconds = time - (minutes * 60)

        val minutesStr = minutes.toString().let { if (it.length < 2) "0$it" else it }
        val secondsStr = seconds.toString().let { if (it.length < 2) "0$it" else it }
        return "$minutesStr:$secondsStr"
    }

    private var timer: Timer? = null

    fun playPause() {
        if (_isPlaying.value.not()) {
            _isPlaying.value = true
            Timer().also {
                timer = it
            }.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    _currentTime.value = _currentTime.value + 1
                }

            }, 1000, 1000)
            saliPlayer.playLayers(data.toList(), true) {
                if (it == PlayerState.PAUSED) {
                    pause()
                }
            }
            animateJob = viewModelScope.launch {
                while (true) {
                    moveScaleImages()
                    delay(500)
                }
            }
        } else {
            pause()
        }
    }

    private fun pause() {
        if (_isPlaying.value.not()) return

        _isPlaying.value = false
        animateJob?.cancel()
        _currentTime.value = 0L
        timer?.cancel()
        saliPlayer.stopPlaying()
    }

    fun rewind() {

    }

    fun fastForward() {

    }

    private fun generateRandomImages(): List<VisualizerImage> {
        val selectedIds = List(30) { ids[random.nextInt(ids.size)] }
        return selectedIds.map { VisualizerImage(it, 1f,  random.nextInt(360), random.nextPosition(), random.nextPosition()) }
    }

    private fun moveScaleImages() {
        _images.value = _images.value.map {
            VisualizerImage(it.id,
                random.nextInt(3, 20).toFloat() / 5,
                random.nextInt(360), random.nextPosition(),
                random.nextPosition()
            )
        }
    }

    private fun Random.nextPosition(): Float {
        return nextFloat()
    }

    override fun onCleared() {
        super.onCleared()
        saliPlayer.stopPlaying()
    }

    enum class State {
        RECORDING,
        LISTENING,
    }

    data class VisualizerImage(
        val id: Int,
        val scale: Float,
        val angle: Int,
        val x: Float,
        val y: Float,
    )
}