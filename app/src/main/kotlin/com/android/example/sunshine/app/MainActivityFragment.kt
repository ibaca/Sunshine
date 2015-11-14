package com.android.example.sunshine.app

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import butterknife.bindView
import com.android.example.sunshine.app.BuildConfig.OPENWEATHERMAP_APIKEY
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.lang.Math.round
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivityFragment : Fragment() {
    val list: RecyclerView by bindView(R.id.listview_forecast)
    val swipe: SwipeRefreshLayout get() = view as SwipeRefreshLayout
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

        swipe.setOnRefreshListener { task() }

        list.setHasFixedSize(true)
        list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        task = { FetchWeatherTask({ list.adapter = ForecastAdapter(it); done() }).execute(location()) }

        task() // initial load
    }

    fun done() = swipe.apply { isRefreshing = false }

    fun location() = location(context)

    fun units() = units(context)

    data class Forecast(val summary: String, val icon: String)

    inner class FetchWeatherTask(val postExecute: (result: List<Forecast>) -> Unit) : AsyncTask<String, Void, List<Forecast>>() {
        val apiKey = OPENWEATHERMAP_APIKEY
        val units = this@MainActivityFragment.units()
        val dateFormat = SimpleDateFormat("EEE MMM dd")
        val base = "http://api.openweathermap.org/data/2.5/forecast/daily"
        val days = 7

        override fun doInBackground(vararg params: String) = parse(URL(uri(params[0])).readText())

        override fun onPostExecute(result: List<Forecast>) = postExecute.invoke(result)

        private fun uri(q: String) = "$base?q=$q&units=metric&cnt=$days&appid=$apiKey"

        private fun formatHighLows(high: Double, low: Double) = "${round(high)}/${round(low)}"

        /** Parse json response and return weather data */
        private fun parse(forecastJsonStr: String): List<Forecast> {
            val forecastJson = JSONObject(forecastJsonStr)
            val weatherArray = forecastJson.getJSONArray("list")
            val date = GregorianCalendar()
            date.set(Calendar.HOUR, 0)
            date.set(Calendar.MINUTE, 0)
            date.set(Calendar.SECOND, 0)
            date.set(Calendar.MILLISECOND, 0)

            return (0..weatherArray.length() - 1).map { i ->
                val dayForecast = weatherArray.getJSONObject(i)
                val day: String = dateFormat.format(date.time)
                date.roll(Calendar.DATE, false)

                // description is in a child array called "weather", which is 1 element long.
                val weather = dayForecast.getJSONArray("weather").getJSONObject(0)
                val description: String = weather.getString("main")
                val icon = weather.getString("icon")

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

                val highAndLow: String = formatHighLows(high, low)
                Forecast("$day - $description - $highAndLow", icon)
            }
        }
    }

    inner class ForecastAdapter(val data: List<Forecast>) : RecyclerView.Adapter<ForecastAdapter.ViewHolder>() {
        val inflater = LayoutInflater.from(context)
        val picasso = Picasso.with(context)

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val image: ImageView by bindView(R.id.item_image)
            val summary: TextView by bindView(R.id.item_summary)

            fun update(forecast: Forecast) {
                forecast.icon.apply { picasso.load(iconUrl(this)).into(image) }
                forecast.summary.apply { summary.text = this }
                itemView.setOnClickListener {
                    startActivity(Intent(context, DetailActivity::class.java)
                            .putExtra(Intent.EXTRA_TEXT, forecast.summary))
                }
            }

            fun iconUrl(icon: String) = "http://openweathermap.org/img/w/$icon.png"
        }


        override fun onBindViewHolder(holder: ForecastAdapter.ViewHolder, position: Int) {
            holder.update (data[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder? {
            return ViewHolder(inflater.inflate(R.layout.list_item_forecast, null))
        }

        override fun getItemCount(): Int = data.size
    }
}
