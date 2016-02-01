package com.android.example.sunshine.app

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import butterknife.bindView
import com.android.example.sunshine.app.data.WeatherContract
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry.buildWeatherLocationAndDate
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate
import com.android.example.sunshine.app.sync.SunshineSyncAdapter
import com.squareup.picasso.Picasso
import java.lang.System.currentTimeMillis
import java.text.SimpleDateFormat

class MainActivityFragment : Fragment() {
    val list: RecyclerView by lazy { view as RecyclerView }
    lateinit var loaderCallback: LoaderManager.LoaderCallbacks<Cursor>
    var useTodayLayout = false
    var forecastAdapter: ForecastAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
        R.id.action_map -> openPreferredLocationInMap().let { true }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onActivityCreated(state: Bundle?) {
        super.onActivityCreated(state)

        forecastAdapter = ForecastAdapter()
        val swap: (Cursor?) -> Unit = { cursor -> forecastAdapter?.swapCursor(cursor) }

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

    fun fetch() = SunshineSyncAdapter.syncImmediately(activity)

    fun location() = location(context)

    fun units() = units(context)

    private fun openPreferredLocationInMap() {
        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        forecastAdapter?.apply {
            cursor?.apply {
                moveToPosition(0)
                val values = asContentValues()
                val posLat = values.getAsString(WeatherContract.LocationEntry.COLUMN_COORD_LAT)
                val posLong = values.getAsString(WeatherContract.LocationEntry.COLUMN_COORD_LON)
                val geoLocation = Uri.parse("geo:$posLat,$posLong")

                val intent = Intent(Intent.ACTION_VIEW).setData(geoLocation)
                if (intent.resolveActivity(activity.packageManager) != null) {
                    startActivity(intent)
                } else {
                    Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!")
                }
            }
        }
    }

    data class Forecast(
            val location: String,
            val date: Long,
            val summary: String,
            val icon: String,
            val min: Double,
            val max: Double
    )

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