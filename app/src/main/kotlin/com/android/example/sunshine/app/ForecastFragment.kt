package com.android.example.sunshine.app

import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.fragment_main.listview_forecast
import org.json.JSONObject
import java.lang.Math.round
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class ForecastFragment : Fragment() {
    var task: (String) -> Unit = { };

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.forecastfragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_refresh -> task("29018,ES").let { true }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onActivityCreated(state: Bundle?) {
        super.onActivityCreated(state)

        val apiKey = context.getPackageManager()
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData.getString("org.openweathermap.API_KEY")

        val weekForecast = ArrayList(Arrays.asList(
                "Today - Sunny - 25/20",
                "Tomorrow - Foggy - 21/19",
                "Weds - Cloudy - 20/16",
                "Thurs - Rainy - 21/16",
                "Fri - Foggy - 22/19",
                "Sat - Sunny - 25/16",
                "Sun - Sunny - 26/17"))

        val listLayout = R.layout.list_item_forecast
        val listView = R.id.list_item_forecast_textview
        val provider = ArrayAdapter(context, listLayout, listView, weekForecast)
        listview_forecast.adapter = provider

        task = { query: String -> FetchWeatherTask(apiKey, provider).execute(query) }
    }

    private class FetchWeatherTask(val apiKey: String, val provider: ArrayAdapter<String>) : AsyncTask<String, Void, List<String>>() {
        val dateFormat = SimpleDateFormat("EEE MMM dd")
        val base = "http://api.openweathermap.org/data/2.5/forecast/daily"
        val units = "metric"
        val days = 7

        override fun onPreExecute() = provider.clear()

        override fun doInBackground(vararg params: String) = parse(URL(uri(params[0])).readText())

        override fun onPostExecute(r: List<String>) = r.forEach { provider.add(it) }

        private fun uri(q: String) = "$base?q=$q&units=$units&cnt=$days&appid=$apiKey"

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
                val high = temperatureObject.getDouble("max")
                val low = temperatureObject.getDouble("min")

                highAndLow = formatHighLows(high, low)
                "$day - $description - $highAndLow"
            }
        }
    }
}
