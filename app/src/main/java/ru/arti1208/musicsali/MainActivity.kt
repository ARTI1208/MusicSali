package ru.arti1208.musicsali

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import androidx.core.view.setPadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.arti1208.musicsali.models.AssetSample
import ru.arti1208.musicsali.models.Instrument
import ru.arti1208.musicsali.ui.Root
import ru.arti1208.musicsali.ui.theme.MusicSaliTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private val viewModel: ScreenViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return ScreenViewModel(
                    AndroidSaliPlayer(assets),
                    AndroidAudioMixer(assets, applicationContext),
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val instruments = assets.list("samples")?.map { instrument ->
            val samples = assets.list("samples/$instrument")?.filter {
                it.endsWith(".wav")
            }?.map { AssetSample(it.removeSuffix(".wav"), "samples/$instrument/$it") } ?: emptyList()
            Instrument(instrument, "", samples)
        } ?: emptyList()

        // cleanup previous shared data
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.intent.collect { listened ->
//                        Log.d("ffrefref", "collected $listened")
//
//                    val file = File.createTempFile("YourAwesomeTrack", ".wav")
//                    FileOutputStream(file, false).use {
//                        with(WavPcmWriter) {
//                            it.writePcmData(listened, 2, 44100, 16)
//                        }
//                    }
//                    val uri = FileProvider.getUriForFile(
//                        this@MainActivity,
//                        applicationContext.packageName + ".provider",
//                        file,
//                    )
//
//                    val intent = Intent(Intent.ACTION_SEND)
//                    intent.setDataAndType(uri, "audio/*")
//                    intent.putExtra(Intent.EXTRA_STREAM, uri)
//                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                    val chooser = Intent.createChooser(intent, "Share awesome track!")
//
//                    startActivity(chooser)
//                }

                viewModel.fileIntent.collect { pcmFile ->
                    Log.d("ffrefref", "collected $pcmFile")

                    val editText = EditText(this@MainActivity).apply {
                        setText("AwesomeTrack.wav")
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    }

                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setView(editText)
                        .setNegativeButton("Cancel") { _, _ -> }
                        .setPositiveButton("OK") { _, _ ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val file = File(cacheDir, editText.text.toString())
                                FileOutputStream(file, false).use {
                                    with(WavPcmWriter) {
                                        it.writePcmData(pcmFile, 2, 44100, 16)
                                    }
                                }
                                pcmFile.delete()

                                val uri = FileProvider.getUriForFile(
                                    this@MainActivity,
                                    applicationContext.packageName + ".provider",
                                    file,
                                )

                                val intent = Intent(Intent.ACTION_SEND)
                                intent.setDataAndType(uri, "audio/*")
                                intent.putExtra(Intent.EXTRA_STREAM, uri)
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                val chooser = Intent.createChooser(intent, "Share awesome track!")

                                startActivity(chooser)
                            }
                        }
                        .create()
                        .show()
                }
            }
        }


        setContent {
            MusicSaliTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Root(instruments, viewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.pauseAll()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MusicSaliTheme {
        Greeting("Android")
    }
}