package com.android.example.sunshine.app

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import butterknife.bindView
import com.android.example.sunshine.app.BuildConfig.OPENWEATHERMAP_APIKEY
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry.buildWeatherLocationAndDate
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.lang.System.currentTimeMillis
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivityFragment : Fragment() {
    var useTodayLayout = false
    val list: RecyclerView by bindView(R.id.listview_forecast)
    val swipe: SwipeRefreshLayout get() = view as SwipeRefreshLayout
    lateinit var loaderCallback: LoaderManager.LoaderCallbacks<Cursor>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_refresh -> fetch().let { true }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onActivityCreated(state: Bundle?) {
        super.onActivityCreated(state)

        val forecastAdapter = ForecastAdapter()
        val swap: (Cursor?) -> Unit = { cursor -> forecastAdapter.swapCursor(cursor) }

        swipe.apply {
            setOnRefreshListener { fetch() }
        }

        list.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = forecastAdapter
        }

        loaderCallback = object : LoaderManager.LoaderCallbacks<Cursor> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> = loader()
            override fun onLoadFinished(l: Loader<Cursor>, data: Cursor) = swap.invoke(data)
            override fun onLoaderReset(l: Loader<Cursor>) = swap.invoke(null)
        }

        loaderManager.initLoader(0, null, loaderCallback)
    }

    public fun onLocationChanged() {
        fetch()
        loaderManager.restartLoader(0, null, loaderCallback)
    }

    private fun loader(): CursorLoader = CursorLoader(context,
            buildWeatherLocationWithStartDate(location(), currentTimeMillis()),
            null, null, null, "${WeatherEntry.COLUMN_DATE} DESC")

    fun fetch() = FetchWeatherTask({ done() }).execute(location()).let { null }

    fun done() = swipe.apply { isRefreshing = false }

    fun location() = location(context)

    fun units() = units(context)

    data class Forecast(
            val location: String,
            val date: Long,
            val summary: String,
            val icon: String,
            val min: Double,
            val max: Double
    )

    inner class FetchWeatherTask(val onComplete: () -> Unit) : AsyncTask<String, Unit, Unit>() {
        val apiKey = OPENWEATHERMAP_APIKEY
        val base = "http://api.openweathermap.org/data/2.5/forecast/daily"
        val days = 14

        override fun doInBackground(vararg params: String): Unit = params[0].let { location ->
            try {
                URL(uri(location)).readText().let { process(location, it) }
            } catch (e: Throwable) {
                Log.e(LOG_TAG, "fetch and process location '$location' failure!, cause: $e", e)
            }
        }

        override fun onPostExecute(result: Unit) = onComplete.invoke()

        private fun uri(q: String) = "$base?q=$q&units=metric&cnt=$days&appid=$apiKey"

        private fun process(locationSetting: String, forecastJsonStr: String): Unit {
            val forecastJson = JSONObject(forecastJsonStr)
            val forecastArray = forecastJson.getJSONArray("list")

            val cityJson = forecastJson.getJSONObject("city")
            val cityName = cityJson.getString("name")

            val cityCoord = cityJson.getJSONObject("coord")
            val cityLatitude = cityCoord.getDouble("lat")
            val cityLongitude = cityCoord.getDouble("lon")

            val locationId = addLocation(context, locationSetting, cityName, cityLatitude, cityLongitude)

            val date = normalizeDate(); date.add(Calendar.DATE, -1)
            val forecastVector = (0..forecastArray.length() - 1).map {
                date.add(Calendar.DATE, 1)
                val dayForecast = forecastArray.getJSONObject(it)

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
                    Log.i(LOG_TAG, "inserting date ${date.time}")
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

            Log.d(LOG_TAG, "FetchWeatherTask Complete. " + forecastVector.size + " inserted")
        }
    }

    inner class ForecastAdapter() : CursorRecyclerAdapter<ForecastAdapter.ViewHolder>() {
        val inflater = LayoutInflater.from(context)
        val picasso = Picasso.with(context)
        val units = this@MainActivityFragment.units()
        val dateFormat = SimpleDateFormat("EEE MMM dd")

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val image: ImageView by bindView(R.id.list_item_icon)
            val date: TextView by bindView(R.id.list_item_date_textview)
            val main: TextView by bindView(R.id.list_item_forecast_textview)
            val high: TextView by bindView(R.id.list_item_high_textview)
            val low: TextView by bindView(R.id.list_item_low_textview)
            val all: Array<TextView> by lazy { arrayOf(date, main, high, low) }

            fun update(forecast: Forecast) {
                forecast.iconUrl.apply {
                    picasso.load(this).into(image, {
                        // itemView.setBackgroundColor(it.rgb)
                        // all.forEach { text -> text.setTextColor(it.bodyTextColor) }
                    })
                }
                forecast.date.apply { date.text = getFriendlyDayString(context, this) }
                forecast.summary.apply { main.text = this; image.contentDescription = this }
                forecast.min.apply { low.text = formatTemperature(context, this, units) }
                forecast.max.apply { high.text = formatTemperature(context, this, units) }

                itemView.setOnClickListener {
                    val uri = buildWeatherLocationAndDate(forecast.location, forecast.date)
                    (activity as Callback).onItemSelected(uri)

                    // Redraw the old selection and the new
                    updateFocusedItem(layoutPosition)
                }
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, cursor: Cursor) {
            cursor.asContentValues().asForecast().apply { holder.update(this) }
        }

        val VT_TODAY = 0;
        val VT_FUTURE = 1;

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(inflater.inflate(layout(viewType), parent, false))
        }

        override fun getItemViewType(position: Int) = when {
            useTodayLayout && position == 0 -> VT_TODAY
            else -> VT_FUTURE
        }

        private fun layout(viewType: Int) = when (viewType) {
            VT_TODAY -> R.layout.list_item_forecast_today
            else -> R.layout.list_item_forecast
        }

        private fun ContentValues.asForecast(): Forecast = asForecast(units, dateFormat)
    }

    interface Callback {
        fun onItemSelected(dateUri: Uri): Unit
    }
}