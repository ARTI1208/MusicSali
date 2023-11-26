package ru.arti1208.musicsali.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.arti1208.musicsali.R
import ru.arti1208.musicsali.ScreenViewModel
import ru.arti1208.musicsali.models.FileSample
import ru.arti1208.musicsali.models.Instrument
import ru.arti1208.musicsali.models.Layer
import ru.arti1208.musicsali.models.LayerState
import ru.arti1208.musicsali.models.Sample
import ru.arti1208.musicsali.models.ScreenState
import java.io.File

@Composable
fun Root(instruments: List<Instrument>, viewModel: ScreenViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {

        val screenState = viewModel.screenStateFlow.collectAsState()

        val instrumentSample = remember { mutableStateOf<Sample?>(null) }
        val layerName = remember { mutableStateOf("") }

        AddLayerDialog(sample = instrumentSample, name = layerName, addLayer = viewModel::addLayer)

        InstrumentsRow(instruments) { _, sample ->
            viewModel.pauseAll()

            layerName.value = sample.name
            instrumentSample.value = sample
        }

        val selectedLayerIndex = viewModel.selectedLayerIndex.collectAsState()
        val selectedLayerState = viewModel.selectedLayerState.collectAsState()

        VolumeAndSpeed(
            selectedLayerState,
            updateVolume = viewModel::setVolume,
            updateSpeed = viewModel::setSpeed,
        )

        LayersList(screenState, selectedLayerIndex, viewModel)

        val hasEnabledLayerState = viewModel.hasEnabledLayers.collectAsState()
        val isRecording = viewModel.isRecording.collectAsState()

        Controls(
            screenState,
            hasEnabledLayerState,
            isRecording,
            { viewModel.addLayer(it) },
            { viewModel.recordMic() },
            { viewModel.recordOrShare() },
            { viewModel.playPauseAll() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstrumentsRow(
    instruments: List<Instrument>,
    onSelected: (Instrument, Sample) -> Unit,
) {

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items(instruments, key = { instrument -> instrument.name }) { instrument ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                val showSamples = remember { mutableStateOf(false) }
                val density = LocalDensity.current

                Image(
                    painter = painterResource(id = R.drawable.guitar),
                    contentDescription = "Instrument: ${instrument.name}",
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = {
                                showSamples.value = true
                            }
                        ) {
                            onSelected(instrument, instrument.samples.first())
                        }
                        .drawBehind {
                            drawCircle(color = Color.LightGray)
                            drawLine(
                                color = Color.Blue,
                                start = Offset(30f, 35f) * density.density,
                                end = Offset(40f, 45f) * density.density,
                                strokeWidth = 5f,
                            )
                            drawLine(
                                color = Color.Blue,
                                start = Offset(50f, 35f) * density.density,
                                end = Offset(40f, 45f) * density.density,
                                strokeWidth = 5f,
                            )
                        }
                        .size(50.dp)
                        .padding(10.dp)
                )
                Text(text = instrument.name)
                DropdownMenu(
                    expanded = showSamples.value,
                    onDismissRequest = { showSamples.value = false }) {
                    instrument.samples.forEach { sample ->
                        DropdownMenuItem(
                            text = { Text(text = sample.name) },
                            modifier = Modifier.drawBehind {
//                                drawRect(color = Color.Gray)
                            }, onClick = {
                                showSamples.value = false
                                onSelected(instrument, sample)
                            })
                    }
                }
            }
        }
    }
}

@Composable
fun VolumeAndSpeed(
    layerState: State<LayerState?>,
    updateVolume: (Float) -> Unit,
    updateSpeed: (Float) -> Unit,
) {

    val currentLayerState = layerState.value ?: return

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = "Volume")
        Slider(
            value = currentLayerState.volume,
            onValueChange = updateVolume,
            valueRange = 0f..1f,
        )
        Text(text = "Speed")
        Slider(
            value = currentLayerState.speed,
            onValueChange = updateSpeed,
            valueRange = 0.1f..5f,
        )
    }
}

@Composable
fun SoundVisualizer(state: State<ScreenState>, selectedLayerIndex: State<Int>) {
//    val visualzer = remember {
//        derivedStateOf {
//            saliPlayer.getVisualizer()
//        }
//    }

//    visualzer.value?.

//    Slider(value = , onValueChange = )
}

private fun startRecord(recorder: MutableState<RecordingData>) {
    val path = File.createTempFile("sali", ".3gpp").absolutePath
    recorder.value = MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFile(path)
        setOutputFormat(MediaRecorder.OutputFormat.AMR_WB)
        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
        setAudioChannels(2)
        setAudioSamplingRate(44100)
        prepare()
        start()
    } to path
}

private typealias RecordingData = Pair<MediaRecorder?, String>

@Composable
fun Controls(
    state: State<ScreenState>,
    hasEnabledLayer: State<Boolean>,
    isRecording: State<Boolean>,
    addLayer: (Layer) -> Unit,
    recordMic: () -> Unit,
    recordOrStopTrack: () -> Unit,
    playPause: () -> Unit,
) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        modifier = Modifier.fillMaxWidth(),
    ) {
        val recorder = remember {
                mutableStateOf<RecordingData>(null to "")
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                startRecord(recorder)
            } else {
                Log.d("DDDDDD","PERMISSION DENIED")
            }
        }

        val context = LocalContext.current

        val recordedSample = remember { mutableStateOf<Sample?>(null) }
        val layerName = remember { mutableStateOf("") }

        val isMicRecording = remember {
            derivedStateOf { recorder.value.first != null }
        }

        Button(
            enabled = !state.value.isPlaying && !isRecording.value,
            onClick = {
            recordMic()
            val mediaRecorder = recorder.value.first
            if (mediaRecorder != null) {
                val path = recorder.value.second
                mediaRecorder.apply {
                    stop()
                    release()
                }

                recorder.value = null to ""

                layerName.value = File(path).nameWithoutExtension
                recordedSample.value = FileSample(path)
            } else {
                when (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)) {
                    PackageManager.PERMISSION_GRANTED -> startRecord(recorder)
                    else -> launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }


        }) {
            Text(text = if (recorder.value.first == null) "Mic" else "Stop")
        }

        AddLayerDialog(sample = recordedSample, name = layerName, addLayer = addLayer)

        Button(
            enabled = hasEnabledLayer.value && !state.value.isPlaying && !isMicRecording.value,
            onClick = recordOrStopTrack,
        ) {
            Text(text = if (isRecording.value) "Share" else "Record track")
        }

        PlayButton(
            state.value.isPlaying,
            enabled = hasEnabledLayer.value && !isMicRecording.value && !isRecording.value,
            playPause,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLayerDialog(
    sample: MutableState<Sample?>,
    name: MutableState<String>,
    addLayer: (Layer) -> Unit,
) {

    val sampleValue = sample.value ?: return

    AlertDialog(
        onDismissRequest = { sample.value = null },
        text = {
            TextField(value = name.value, onValueChange = { name.value = it })
        },
        confirmButton = {
            TextButton(enabled = name.value.isNotBlank(), onClick = {
                addLayer(Layer(sampleValue, name.value))
                sample.value = null
            }) {
                Text("Confirm")
            }
        }
    )
}

@Composable
fun LayersList(
    state: State<ScreenState>,
    selectedLayerIndex: State<Int>,
    viewModel: ScreenViewModel,
) {
    val layers = viewModel.layersFlow.collectAsState()
    LazyColumn {
        itemsIndexed(layers.value) { index, layer ->
            val layerState = viewModel.getLayerState(layer)!!.collectAsState()
            LayerRow(layer, layerState.value, modifier = Modifier
                .clickable {
                    viewModel.selectLayer(index)
                }
                .drawBehind {
                    if (selectedLayerIndex.value == index) {
                        drawRect(Color.LightGray)
                    } else {
//                        drawRect(Color.Gray)
                    }
                },
                playPause = { viewModel.playPauseLayer(layer) },
                flipEnabled = { viewModel.enableLayer(layer, layerState.value.enabled.not()) }
            ) {
                if (index == state.value.layers.lastIndex) {
                    viewModel.selectLayer(index - 1)
                }
                viewModel.removeLayer(layer)
            }
        }
    }
}

@Composable
fun LayerRow(
    layer: Layer,
    layerState: LayerState,
    modifier: Modifier,
    playPause: () -> Unit,
    flipEnabled: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = layer.name, modifier = Modifier.fillMaxWidth())
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PlayButton(layerState.isPlaying, enabled = true, playPause)
            Button(onClick = flipEnabled) {
                Text(text = if (layerState.enabled) "Disable" else "Enable")
            }
            Button(onClick = onRemove) {
                Text(text = "Remove")
            }
        }
    }
}

@Composable
fun PlayButton(isPlaying: Boolean, enabled: Boolean, playPause: () -> Unit) {
    Button(
        enabled = enabled,
        onClick = playPause,
    ) {
        Text(text = if (isPlaying) "Pause" else "Play")
    }
}