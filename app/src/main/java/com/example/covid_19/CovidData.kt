package com.example.covid_19

import com.google.gson.annotations.SerializedName
import java.util.*

data class CovidData(
    val dateChecked: Date,
    val positiveIncrease: Int,
    val negativeIncrease: Int,
    val deathIncrease: Int,
    val state: String
        )
