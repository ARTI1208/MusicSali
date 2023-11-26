package ru.arti1208.musicsali

import ru.arti1208.musicsali.models.Layer

sealed interface SaliAction

object RecordMic : SaliAction

object RecordOrShare : SaliAction

class SetSpeed(val speed: Float) : SaliAction

class SetVolume(val volume: Float) : SaliAction

class EnableLayer(val layer: Layer, val enable: Boolean) : SaliAction

class AddLayer(val layer: Layer) : SaliAction

class RemoveLayer(val layer: Layer) : SaliAction

class PlayPauseLayer(val layer: Layer) : SaliAction

object PlayPauseAll : SaliAction

object PauseAll : SaliAction
