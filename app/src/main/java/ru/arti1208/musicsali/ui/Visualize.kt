package ru.arti1208.musicsali.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.arti1208.musicsali.R
import ru.arti1208.musicsali.VisualizeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizeRoot(visualizeViewModel: VisualizeViewModel) {
    Column {

        val showControls = remember {
            mutableStateOf(true)
        }

        val showControlsJob = remember {
            mutableStateOf(null as Job?)
        }

        val isPlaying = visualizeViewModel.isPlaying.collectAsState()
        LaunchedEffect(key1 = isPlaying.value) {
            val shouldShow = isPlaying.value.not()
            showControlsJob.value?.cancel()
            showControlsJob.value = launch {
                if (shouldShow.not()) {
                    delay(3000)
                }
                showControls.value = shouldShow
            }
        }

        val scope = rememberCoroutineScope()

        if (showControls.value) {
            AppBar()
        }
        Visualizer(visualizeViewModel) {
            showControlsJob.value?.cancel()
            showControls.value = true
            showControlsJob.value = scope.launch {
                delay(3000)
                showControls.value = false
            }
        }

        if (showControls.value) {
            BottomBar(visualizeViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar() {
    val activity = LocalContext.current as Activity
    TopAppBar(title = { Text("Название трека") }, navigationIcon = { IconButton(onClick = { activity.finish() }) {
        Image(painter = painterResource(id = R.drawable.back), contentDescription = "")
    } }, actions = { IconButton(onClick = { Toast.makeText(activity, "Not implemented :(", Toast.LENGTH_SHORT).show() }) {
        Image(painter = painterResource(id = R.drawable.export), contentDescription = "")
    } })
}

@Composable
private fun ColumnScope.Visualizer(viewModel: VisualizeViewModel, onTapped: () -> Unit) {
    val images = viewModel.images.collectAsState()
//    val bitmaps = images.value.map { ImageBitmap.imageResource(id = it.id) }
    val vectors = images.value.map { ImageVector.vectorResource(id = it.id) }

    val painters = vectors.map { rememberVectorPainter(image = it) }

    var canvasHeight = 0
    var canvasWidth = 0
    var canvasX = 0
    var canvasY = 0

    val scales = remember {
        images.value.map { Animatable(it.scale) }
    }

    val x = remember {
        images.value.map { Animatable(it.x.toFloat()) }
    }

    val y = remember {
        images.value.map { Animatable(it.y.toFloat()) }
    }

    val angles = remember {
        images.value.map { Animatable(it.angle.toFloat()) }
    }

    images.value.forEachIndexed { index, visualizerImage ->

        LaunchedEffect(visualizerImage, visualizerImage.scale) {
            launch {
                scales[index].animateTo(visualizerImage.scale)
            }
        }

        LaunchedEffect(visualizerImage, visualizerImage.x) {
            launch {
                x[index].animateTo((visualizerImage.x * canvasWidth).coerceAtMost(canvasWidth.toFloat()))
            }
        }

        LaunchedEffect(visualizerImage, visualizerImage.y) {
            launch {
                println("y: ${visualizerImage.y}")
                y[index].animateTo((visualizerImage.y * canvasHeight).coerceAtMost(canvasHeight.toFloat()))
            }
        }
    }

    Canvas(modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .onGloballyPositioned {
            canvasHeight = it.size.height
            canvasWidth = it.size.width
        }
        .clipToBounds()
        .clickable { onTapped() }) {
        painters.forEachIndexed { index, painter ->
            val scale = scales[index].value
            val x = x[index].value
            val y = y[index].value
            val angle = angles[index].value
//            println("ch: $canvasHeight; cw: $canvasWidth; x: $x; y: $y")



            withTransform({
                translate(left = x, top = y)
                rotate(angle, pivot = Offset(
                    painter.intrinsicSize.width / 2,
                    painter.intrinsicSize.height / 2
                ))
                scale(scale)
            }) {
                with(painter) {
                    draw(painter.intrinsicSize)
                }
            }
        }
    }
}

@Composable
private fun BottomBar(visualizeViewModel: VisualizeViewModel) {
    val state = visualizeViewModel.state.collectAsState()
    val currentTimeStr = visualizeViewModel.currentTimeStr.collectAsState("00:00")

    if (state.value == VisualizeViewModel.State.RECORDING) {
        Text(text = currentTimeStr.value)
    } else {
        val currentTime = visualizeViewModel.currentTime.collectAsState()
        Slider(value = currentTime.value.toFloat(), onValueChange = {

        })
        Row {
            Text(text = currentTimeStr.value)
        }
    }
    val isPlaying = visualizeViewModel.isPlaying.collectAsState()
    Button(onClick = { visualizeViewModel.playPause() }) {
        Text(text = if (isPlaying.value) "Pause"  else "Play")
    }
}