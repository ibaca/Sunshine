package com.android.example.sunshine.app

import android.content.ContentValues
import android.content.Context
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
import com.android.example.sunshine.app.data.WeatherContract.LocationEntry
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry
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
        val days = 14

        override fun doInBackground(vararg params: String): List<Forecast> {
            val locationSetting = params[0]
            return parse(locationSetting, URL(uri(locationSetting)).readText())
        }

        override fun onPostExecute(result: List<Forecast>) = postExecute.invoke(result)

        private fun uri(q: String) = "$base?q=$q&units=metric&cnt=$days&appid=$apiKey"

        private fun formatHighLows(high: Double, low: Double) = "${round(high)}/${round(low)}"

        /** Parse json response and return weather data */
        private fun parse(locationSetting: String, forecastJsonStr: String): List<Forecast> {
            val forecastJson = JSONObject(forecastJsonStr)
            val forecastArray = forecastJson.getJSONArray("list")

            val cityJson = forecastJson.getJSONObject("city")
            val cityName = cityJson.getString("name")

            val cityCoord = cityJson.getJSONObject("coord")
            val cityLatitude = cityCoord.getDouble("lat")
            val cityLongitude = cityCoord.getDouble("lon")

            val locationId = addLocation(context, locationSetting, cityName, cityLatitude, cityLongitude)

            val date = normalizeDate()
            val forecastVector = (0..forecastArray.length() - 1).map {
                val dayForecast = forecastArray.getJSONObject(it)
                date.roll(Calendar.DATE, false)

                val pressure = dayForecast.getDouble("pressure")
                val humidity = dayForecast.getInt("humidity")
                val windSpeed = dayForecast.getDouble("speed")
                val windDirection = dayForecast.getDouble("deg")

                // description is in a child array called "weather", which is 1 element long.
                val weatherArray = dayForecast.getJSONArray("weather").getJSONObject(0)
                val description: String = weatherArray.getString("main")
                val weatherId = weatherArray.getString("id")
                val icon = weatherArray.getString("icon")

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                val temperatureObject = dayForecast.getJSONObject("temp")
                var high = temperatureObject.getDouble("max")
                var low = temperatureObject.getDouble("min")

                ContentValues().apply {
                    Log.i(LOG_TAG, "inserting date $date")
                    put(WeatherEntry.COLUMN_LOC_KEY, locationId)
                    put(WeatherEntry.COLUMN_DATE, date.timeInMillis)
                    put(WeatherEntry.COLUMN_HUMIDITY, humidity)
                    put(WeatherEntry.COLUMN_PRESSURE, pressure)
                    put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed)
                    put(WeatherEntry.COLUMN_DEGREES, windDirection)
                    put(WeatherEntry.COLUMN_MAX_TEMP, high)
                    put(WeatherEntry.COLUMN_MIN_TEMP, low)
                    put(WeatherEntry.COLUMN_SHORT_DESC, description)
                    put(WeatherEntry.COLUMN_WEATHER_ID, weatherId)
                    put(WeatherEntry.COLUMN_ICON, icon)
                }
            }.toTypedArray()

            // add to database
            if (forecastVector.size > 0) {
                context.contentResolver.bulkInsert(WeatherEntry.CONTENT_URI, forecastVector)
            }

            val result = context.contentResolver
                    .query(WeatherEntry.CONTENT_URI, null,
                            "${WeatherEntry.COLUMN_DATE} <= ?",
                            arrayOf(normalizeDate().timeInMillis.toString()),
                            "${WeatherEntry.COLUMN_DATE} DESC")
                    .use { it.asContentValuesVector() }

            Log.d(LOG_TAG, "FetchWeatherTask Complete. " + result.size + " inserted")
            return result.map { asForecast(it) }
        }

        private fun asForecast(forecast: ContentValues): Forecast {
            var high = forecast.getAsDouble(WeatherEntry.COLUMN_MAX_TEMP)
            var low = forecast.getAsDouble(WeatherEntry.COLUMN_MIN_TEMP)
            if (units.equals(getString(R.string.pref_units_imperial))) {
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            } else if (!units.equals(getString(R.string.pref_units_metric))) {
                Log.d(LOG_TAG, "Unsupported unit type: " + units);
            }
            val day: String = dateFormat.format(forecast.getAsLong(WeatherEntry.COLUMN_DATE))
            val description = forecast.getAsString(WeatherEntry.COLUMN_SHORT_DESC)
            val icon = forecast.getAsString(WeatherEntry.COLUMN_ICON)
            return Forecast("$day - $description - ${formatHighLows(high, low)}", icon)
        }
    }

    inner class ForecastAdapter(val data: List<Forecast>) : RecyclerView.Adapter<ForecastAdapter.ViewHolder>() {
        val inflater = LayoutInflater.from(context)
        val picasso = Picasso.with(context)

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val image: ImageView by bindView(R.id.item_image)
            val summary: TextView by bindView(R.id.item_summary)

            fun update(forecast: Forecast) {
                forecast.icon.apply {
                    picasso.load(iconUrl(this)).into(image, {
                        itemView.setBackgroundColor(it.rgb)
                        summary.setTextColor(it.bodyTextColor)
                    })
                }
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
            return ViewHolder(inflater.inflate(R.layout.list_item_forecast, parent, false))
        }

        override fun getItemCount(): Int = data.size
    }
}

fun addLocation(context: Context, locationSetting: String, cityName: String, lat: Double, lon: Double): Long {
    // First, check if the location with this city name exists in the db
    context.contentResolver.query(
            LocationEntry.CONTENT_URI,
            arrayOf(LocationEntry._ID),
            "${LocationEntry.COLUMN_LOCATION_SETTING} = ?",
            arrayOf(locationSetting),
            null
    ).use {
        if (it.moveToFirst()) {
            // If it exists, return the current ID
            return it.getLong(it.getColumnIndex(LocationEntry._ID))
        } else {
            // Otherwise, insert it using the content resolver and the base URI
            return context.contentResolver.insert(LocationEntry.CONTENT_URI, ContentValues().apply {
                put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting)
                put(LocationEntry.COLUMN_CITY_NAME, cityName)
                put(LocationEntry.COLUMN_COORD_LAT, lat)
                put(LocationEntry.COLUMN_COORD_LON, lon)
            }).contentId()
        }
    }
}
