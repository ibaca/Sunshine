@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("UtilKt")

package com.android.example.sunshine.app

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.preference.PreferenceManager
import android.support.v7.graphics.Palette
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
        c.getString(R.string.pref_location_key), c.getString(R.string.pref_location_default))

fun units(c: Context): String = PreferenceManager.getDefaultSharedPreferences(c).getString(
        c.getString(R.string.pref_units_key), c.getString(R.string.pref_units_metric))

/** To make it easy to query for the exact date, normalize all database dates. */
fun normalizeDate(dateInMillis: Long = System.currentTimeMillis()) = GregorianCalendar(UTC).apply {
    timeInMillis = dateInMillis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

val UTC by lazy { TimeZone.getTimeZone("UTC") }

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

fun formatHighLows(high: Double, low: Double) = "${Math.round(high)}/${Math.round(low)}"

fun ContentValues.asForecast(units: String, dateFormat: Format = SimpleDateFormat("EEE MMM dd")): MainActivityFragment.Forecast {
    var high = this.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)
    var low = this.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)

    // TODO use context.getString(R.string.pref_units_imperial)
    val IM = "imperial";
    val ME = "metric"

    if (units.equals(IM)) {
        high = (high * 1.8) + 32; low = (low * 1.8) + 32;
    } else if (!units.equals(ME)) {
        Log.d(LOG_TAG, "Unsupported unit type: " + units)
    }

    val day: String = dateFormat.format(this.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE))
    val description = this.getAsString(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC)

    return MainActivityFragment.Forecast(
            this.getAsString(LocationEntry.COLUMN_LOCATION_SETTING),
            this.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE),
            "$day - $description - ${formatHighLows(high, low)}",
            this.getAsString(WeatherContract.WeatherEntry.COLUMN_ICON))
}
