package ru.arti1208.musicsali.models

data class Instrument(
    val name: String,
    val image: String,
    val samples: List<Sample>,
)
