package com.android.example.sunshine.app

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import com.android.example.sunshine.app.BuildConfig.OPENWEATHERMAP_APIKEY
import kotlinx.android.synthetic.fragment_main.listview_forecast
import org.json.JSONObject
import java.lang.Math.round
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivityFragment : Fragment() {
    val LOG_TAG = MainActivityFragment::class.java.simpleName

    var task: () -> Unit = { };

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_refresh -> task().let { true }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onActivityCreated(state: Bundle?) {
        super.onActivityCreated(state)

        val listLayout = R.layout.list_item_forecast
        val listView = R.id.list_item_forecast_textview
        val provider = ArrayAdapter(context, listLayout, listView, ArrayList<String>())
        listview_forecast.adapter = provider
        listview_forecast.setOnItemClickListener { adapterView, view, i, l ->
            startActivity(Intent(context, DetailActivity::class.java)
                    .putExtra(Intent.EXTRA_TEXT, provider.getItem(i)))
        }

        (view as SwipeRefreshLayout).setOnRefreshListener { task() }

        task = { FetchWeatherTask(provider).execute(location()) }

        task() // initial load
    }

    fun done() { (view as SwipeRefreshLayout).isRefreshing = false }

    fun location() = location(context)

    fun units() = units(context)

    inner class FetchWeatherTask(val provider: ArrayAdapter<String>) : AsyncTask<String, Void, List<String>>() {
        val apiKey = OPENWEATHERMAP_APIKEY
        val units = this@MainActivityFragment.units()
        val dateFormat = SimpleDateFormat("EEE MMM dd")
        val base = "http://api.openweathermap.org/data/2.5/forecast/daily"
        val days = 7

        override fun doInBackground(vararg params: String) = parse(URL(uri(params[0])).readText())

        override fun onPostExecute(result: List<String>) { provider.addIt(result); done() }

        private fun uri(q: String) = "$base?q=$q&units=metric&cnt=$days&appid=$apiKey"

        private fun formatHighLows(high: Double, low: Double) = "${round(high)}/${round(low)}"

        /** Parse json response and return weather data */
        private fun parse(forecastJsonStr: String): List<String> {
            val forecastJson = JSONObject(forecastJsonStr)
            val weatherArray = forecastJson.getJSONArray("list")
            val date = GregorianCalendar()
            date.set(Calendar.HOUR, 0)
            date.set(Calendar.MINUTE, 0)
            date.set(Calendar.SECOND, 0)
            date.set(Calendar.MILLISECOND, 0)

            return (0..weatherArray.length() - 1).map { i ->
                // For now, using the format "Day, description, hi/low"
                val day: String
                val description: String
                val highAndLow: String

                val dayForecast = weatherArray.getJSONObject(i)

                day = dateFormat.format(date.time)
                date.roll(Calendar.DATE, false)

                // description is in a child array called "weather", which is 1 element long.
                val weatherObject = dayForecast.getJSONArray("weather").getJSONObject(0)
                description = weatherObject.getString("main")

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                val temperatureObject = dayForecast.getJSONObject("temp")
                var high = temperatureObject.getDouble("max")
                var low = temperatureObject.getDouble("min")


                if (units.equals(getString(R.string.pref_units_imperial))) {
                    high = (high * 1.8) + 32;
                    low = (low * 1.8) + 32;
                } else if (!units.equals(getString(R.string.pref_units_metric))) {
                    Log.d(LOG_TAG, "Unsupported unit type: " + units);
                }

                highAndLow = formatHighLows(high, low)
                "$day - $description - $highAndLow"
            }
        }
    }
}
