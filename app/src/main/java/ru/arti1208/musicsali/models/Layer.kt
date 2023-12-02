package ru.arti1208.musicsali.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
/**data**/ class Layer(
    val sample: Sample,
    val name: String = sample.name,
): Parcelable
