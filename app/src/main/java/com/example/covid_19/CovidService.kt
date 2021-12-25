package com.example.covid_19


import retrofit2.Call
import retrofit2.http.GET

interface CovidService {

    @GET("us/daily.json")
    fun getNationalData(): Call<List<CovidData>>



    @GET("states/daily.json")
    fun getStatesData(): Call<List<CovidData>>
}