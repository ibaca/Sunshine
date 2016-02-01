@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("UtilKt")

package com.android.example.sunshine.app

import android.app.Activity
import android.content.*
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v7.graphics.Palette
import android.text.format.Time
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.android.example.sunshine.app.data.WeatherContract
import com.android.example.sunshine.app.data.WeatherContract.LocationEntry
import com.squareup.picasso.Callback
import com.squareup.picasso.RequestCreator
import rx.subscriptions.CompositeSubscription
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

const val LOG_TAG = "SUNSHINE"

fun Activity.start(type: KClass<*>): Boolean {
    startActivity(Intent(this, type.java))
    return true
}

fun Fragment.start(type: KClass<*>): Boolean {
    startActivity(Intent(context, type.java))
    return true
}

fun Activity.start(action: String, uri: String): Boolean {
    try {
        val intent = Intent(action, Uri.parse(uri))
        if (intent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "Your android sucks!", Toast.LENGTH_LONG)
        } else {
            startActivity(intent)
        }
    } catch (e: Exception) {
        Toast.makeText(this, "Something goes wong!", Toast.LENGTH_LONG)
        Log.e(this.javaClass.simpleName, "Error starting Intent{action=$action, uri=$uri", e)
    }
    return true
}

fun FragmentManager.tx(f: FragmentTransaction.() -> Unit): Unit {
    beginTransaction().apply(f).commit()
}

fun ContentValues.populate(cursor: Cursor): ContentValues = apply {
    DatabaseUtils.cursorRowToContentValues(cursor, this)
}

fun Cursor.asContentValues(values: ContentValues = ContentValues()) = values.populate(this)

fun Cursor.next() = if (moveToNext()) this else null;

fun Cursor.first() = if (moveToFirst()) this else throw NoSuchElementException("empty cursor")

fun Cursor.asContentValuesVector(): Vector<ContentValues> {
    var vector = Vector<ContentValues>(count)
    if (moveToFirst()) {
        do {
            vector.add(this.asContentValues())
        } while (moveToNext())
    }
    return vector;
}

fun ContentResolver.query(uri: Uri,
                          projection: Array<String>? = null,
                          selection: String? = null,
                          selectionArgs: Array<String>? = null,
                          sortOrder: String? = null): Cursor {
    return this.query(uri, projection, selection, selectionArgs, sortOrder);
}

fun SQLiteDatabase.query(cursorFactory: SQLiteDatabase.CursorFactory? = null,
                         distinct: Boolean = false,
                         table: String,
                         columns: Array<String>? = null,
                         selection: String? = null,
                         selectionArgs: Array<String>? = null,
                         groupBy: String? = null,
                         having: String? = null,
                         orderBy: String? = null,
                         limit: String? = null): Cursor {
    return queryWithFactory(cursorFactory, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
}

fun RequestCreator.into(target: ImageView, callback: (Palette.Swatch) -> Unit) {
    into(target, object : Callback {
        override fun onSuccess() {
            Palette.from((target.drawable as BitmapDrawable).bitmap)
                    .generate({ callback.invoke(it.swatches[0]) })
        }

        override fun onError() {
            Log.e(LOG_TAG, "forecast icon load error")
        }
    })
}

fun Uri.contentId() = ContentUris.parseId(this)

fun CompositeSubscription.subscribe(o: rx.Observable<Any>) = apply { add(o.subscribe()) }

fun location(c: Context): String = PreferenceManager.getDefaultSharedPreferences(c).getString(
        c.getString(R.string.pref_location_key),
        c.getString(R.string.pref_location_default))

fun units(c: Context): String = PreferenceManager.getDefaultSharedPreferences(c).getString(
        c.getString(R.string.pref_units_key),
        c.getString(R.string.pref_units_default))

fun notifications(c: Context) = PreferenceManager.getDefaultSharedPreferences(c).getBoolean(
        c.getString(R.string.pref_notifications_key),
        c.getString(R.string.pref_notifications_default).toBoolean())

fun metric(c: Context): Boolean = units(c) == "metric"

/** To make it easy to query for the exact date, normalize all database dates. */
fun normalizeDate(dateInMillis: Long = System.currentTimeMillis()) = GregorianCalendar(UTC).apply {
    timeInMillis = dateInMillis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

val UTC by lazy { TimeZone.getTimeZone("UTC") }

fun addLocation(cr: ContentResolver, locationSetting: String, cityName: String, lat: Double, lon: Double): Long {
    // First, check if the location with this city name exists in the db
    cr.query(
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
            return cr.insert(LocationEntry.CONTENT_URI, ContentValues().apply {
                put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting)
                put(LocationEntry.COLUMN_CITY_NAME, cityName)
                put(LocationEntry.COLUMN_COORD_LAT, lat)
                put(LocationEntry.COLUMN_COORD_LON, lon)
            }).contentId()
        }
    }
}

fun formatTemperature(context: Context, temperature: Double, units: String): String {
    return context.getString(R.string.format_temperature, when (units) {
        "imperial" -> 9 * temperature / 5 + 32
        else -> temperature
    })
}

fun formatHighLows(high: Double, low: Double) = "${Math.round(high)}/${Math.round(low)}"

fun ContentValues.asForecast(unit: String, dateFormat: Format = SimpleDateFormat("EEE MMM dd")): MainActivityFragment.Forecast {
    var high = this.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)
    var low = this.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)

    // TODO use context.getString(R.string.pref_units_imperial)
    val IM = "imperial";
    val ME = "metric"

    if (unit.equals(IM)) {
        high = (high * 1.8) + 32; low = (low * 1.8) + 32;
    } else if (!unit.equals(ME)) {
        Log.d(LOG_TAG, "Unsupported unit type: " + unit)
    }

    val day: String = dateFormat.format(this.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE))
    val description = this.getAsString(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC)

    return MainActivityFragment.Forecast(
            this.getAsString(LocationEntry.COLUMN_LOCATION_SETTING),
            this.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE),
            "$day - $description - ${formatHighLows(high, low)}",
            this.getAsString(WeatherContract.WeatherEntry.COLUMN_ICON),
            this.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP),
            this.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)
    )
}

/**
 * Helper method to convert the database representation of the date into something to display
 * to users.  As classy and polished a user experience as "20140102" is, we can do better.
 */
fun getFriendlyDayString(context: Context, dateInMillis: Long): String {
    // The day string for forecast uses the following logic:
    // For today: "Today, June 8"
    // For tomorrow:  "Tomorrow"
    // For the next 5 days: "Wednesday" (just the day name)
    // For all days after that: "Mon Jun 8"

    val now = Time().apply { setToNow() }
    val currentTime = System.currentTimeMillis()
    val julianDay = Time.getJulianDay(dateInMillis, now.gmtoff)
    val currentJulianDay = Time.getJulianDay(currentTime, now.gmtoff)

    return when {
        julianDay == currentJulianDay -> context.getString(
                R.string.format_full_friendly_date,
                context.getString(R.string.today),
                getFormattedMonthDay(context, dateInMillis))
        julianDay < currentJulianDay + 7 -> getDayName(context, dateInMillis)
        else -> SimpleDateFormat("EEE MMM dd").format(dateInMillis)
    }
}

/** Given a day, returns just the name to use for that day. E.g "today", "tomorrow", "wednesday". */
fun getDayName(context: Context, dateInMillis: Long): String {
    val now = Time().apply { setToNow() }
    val julianDay = Time.getJulianDay(dateInMillis, now.gmtoff)
    val currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), now.gmtoff)

    return when (julianDay) {
        currentJulianDay -> context.getString(R.string.today)
        currentJulianDay + 1 -> context.getString(R.string.tomorrow)
        else -> SimpleDateFormat("EEEE").format(dateInMillis)
    }
}

/** Converts db date format to the format "Month day", e.g "June 24". */
fun getFormattedMonthDay(context: Context, dateInMillis: Long): String {
    return SimpleDateFormat("MMMM dd").format(dateInMillis)
}

fun getFormattedWind(c: Context, windSpeed: Float, degrees: Float): String = c
        .getString(if (metric(c)) R.string.format_wind_kmh else R.string.format_wind_mph)
        .format(if (metric(c)) windSpeed else .621371192237334f * windSpeed, direction(degrees))

private fun direction(degrees: Float) = when {
    degrees >= 337.5 || degrees < 22.5 -> "N"
    degrees >= 22.5 && degrees < 67.5 -> "NE"
    degrees >= 67.5 && degrees < 112.5 -> "E"
    degrees >= 112.5 && degrees < 157.5 -> "SE"
    degrees >= 157.5 && degrees < 202.5 -> "S"
    degrees >= 202.5 && degrees < 247.5 -> "SW"
    degrees >= 247.5 && degrees < 292.5 -> "W"
    degrees >= 292.5 || degrees < 22.5 -> "NW"
    else -> "Unknown"
}

fun weatherIconUrl(icon: String) = "http://openweathermap.org/img/w/$icon.png"
val MainActivityFragment.Forecast.iconUrl: String get() = weatherIconUrl(this.icon)
