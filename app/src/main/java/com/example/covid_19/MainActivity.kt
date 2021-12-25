package com.example.covid_19

import android.widget.TextView
import java.text.NumberFormat
import java.text.SimpleDateFormat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.ticker.TickerUtils
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


import java.util.*

private const val BASE_URL = "https://api.covidtracking.com/v1/"
private const val TAG = "MainActivity"
private const val ALL_STATES = "All (Nationalwide)"

class MainActivity : AppCompatActivity() {

    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private  lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = getString(R.string.app_description)

        //Instance of gson
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        //instance of CovidService
        val  covidService = retrofit.create(CovidService::class.java)

        //Fetch national data
        covidService.getNationalData().enqueue(object: Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.e(TAG, "onResponse $response")
                //check if the data received is valid
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.w(TAG, "Did not receive a valid response body or data")
                    return
                }
                setupEventListeners()
                nationalDailyData = nationalData.reversed() //to get the oldest data first
                Log.i(TAG, "Update graph with national data")
                updateDisplayWithData(nationalDailyData)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")


            }

        })

        //Fetch state data
        covidService.getStatesData().enqueue(object: Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.e(TAG, "onResponse $response")
                //check if the data received is valid
                val statesData = response.body()
                if (statesData == null) {
                    Log.w(TAG, "Did not receive a valid response body or data")
                    return
                }
                perStateDailyData = statesData.reversed().groupBy{it.state} //to get the oldest data first and group them based on state names
                // Update spinner  with state data
                updateSpinnerWithStateData(perStateDailyData.keys) //takes in a map of each day data from a particular state
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")


            }

        })
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        //create an ordered list for state names
        val stateAbbreviationList = stateNames.toMutableList()
        stateAbbreviationList.sort() //sort state names alphabetically
        stateAbbreviationList.add(0,ALL_STATES) // first value of the sorted list as a entire national data

        //Add state list as data source for the spinner
        spinnerSelect.attachDataSource(stateAbbreviationList) //drop down for the states abbreviations
        spinnerSelect.setOnSpinnerItemSelectedListener{parent,_, position,_ ->
        val selectState = parent.getItemAtPosition(position) as String
        val selectedData = perStateDailyData[selectState] ?: nationalDailyData
        updateDisplayWithData(selectedData)}


    }

    private fun setupEventListeners() {
        tickerView.setCharacterLists(TickerUtils.provideNumberList())
        //Add a listener for the user scrubbing on the chart
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener { itemData ->
            if (itemData is CovidData) {
                updateInfoForDate(itemData)
            }
        }
        //Respond to radio button selected events
        radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId ->
        adapter.daysAgo = when (checkedId) {
            R.id.radioButtonWeek -> TimeScale.WEEK
            R.id.radioButtonMonth -> TimeScale.MONTH
            else -> TimeScale.MAX
        }
            //notify the adapter that data have changed
            adapter.notifyDataSetChanged()

        }
        radioGroupMetricSelection.setOnCheckedChangeListener { _, checkedId->
            when(checkedId) {
                R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.radioButtonNegative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.radioButtonDeath -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        //update the color of the chart accordingly
        val colorResource = when (metric) {
            Metric.NEGATIVE -> R.color.colorNegative
            Metric.POSITIVE -> R.color.colorPositive
            Metric.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this,colorResource)
        sparkView.lineColor = colorInt

        //update the textView color
        tickerView.setTextColor(colorInt)
        // Update the metric on the adapter
       adapter.metric = metric
        adapter.notifyDataSetChanged()

        // Reset date and number of cases shown by the two bottom textViews
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
        //create a new SparkAdapter with data
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        //Update radio buttons to select the positive cases and max time by default
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true

        //Display metric for the most recent date
        
        updateDisplayMetric(Metric.POSITIVE) //as default

    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when (adapter.metric) {
            Metric.NEGATIVE -> covidData.negativeIncrease.toFloat()
            Metric.POSITIVE -> covidData.positiveIncrease.toFloat()
            Metric.DEATH -> covidData.deathIncrease.toFloat()
        }
       tickerView.text = NumberFormat.getInstance().format(numCases) //showing based on metric
        val outputDateFormat =  SimpleDateFormat("MM dd, yyyy", Locale.US)
       tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)

    }
}