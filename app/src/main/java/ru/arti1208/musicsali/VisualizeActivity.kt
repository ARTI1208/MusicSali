package ru.arti1208.musicsali

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import ru.arti1208.musicsali.models.Layer
import ru.arti1208.musicsali.models.LayerState
import ru.arti1208.musicsali.ui.VisualizeRoot
import ru.arti1208.musicsali.ui.theme.MusicSaliTheme

class VisualizeActivity : AppCompatActivity() {

    private val viewModel: VisualizeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return VisualizeViewModel(
                    AndroidSaliPlayer(assets),
                    AndroidAudioMixer(assets, applicationContext),
                    run {
                        val states = intent.getParcelableArrayExtra("states")
                        val layers = intent.getParcelableArrayExtra("layers")

                        requireNotNull(states)
                        requireNotNull(layers)

                        buildMap {
                            repeat(layers.size) {
                                put(layers[it] as Layer, states[it] as LayerState)
                            }
                        }
                    },
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MusicSaliTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VisualizeRoot(viewModel)
                }
            }
        }
    }
}