package com.android.example.sunshine.app.data

import android.annotation.TargetApi
import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import com.android.example.sunshine.app.data.WeatherContract.LocationEntry
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry
import com.android.example.sunshine.app.normalizeDate
import com.android.example.sunshine.app.query

class WeatherProvider : ContentProvider() {
    private var mOpenHelper: WeatherDbHelper? = null

    override fun onCreate(): Boolean {
        mOpenHelper = WeatherDbHelper(context)
        return true
    }

    override fun getType(uri: Uri): String = when (uriMatcher.match(uri)) {
        WEATHER -> WeatherEntry.CONTENT_TYPE
        WEATHER_WITH_LOCATION -> WeatherEntry.CONTENT_TYPE
        WEATHER_WITH_LOCATION_AND_DATE -> WeatherEntry.CONTENT_ITEM_TYPE
        LOCATION -> LocationEntry.CONTENT_TYPE
        else -> throw UnsupportedOperationException("Unknown uri: " + uri)
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor {
        return when (uriMatcher.match(uri)) {
            WEATHER_WITH_LOCATION_AND_DATE -> getWeatherByLocationSettingAndDate(uri, projection, sortOrder)
            WEATHER_WITH_LOCATION -> getWeatherByLocationSetting(uri, projection, sortOrder)
            WEATHER -> read().query(
                    table = WeatherEntry.TABLE_NAME,
                    columns = projection,
                    selection = selection,
                    selectionArgs = selectionArgs,
                    orderBy = sortOrder)
            LOCATION -> read().query(
                    table = LocationEntry.TABLE_NAME,
                    columns = projection,
                    selection = selection,
                    selectionArgs = selectionArgs,
                    orderBy = sortOrder)
            else -> throw UnsupportedOperationException("Unknown uri: " + uri)
        }.apply { setNotificationUri(context!!.contentResolver, uri) }
    }

    private fun getWeatherByLocationSetting(uri: Uri, projection: Array<String>?, sortOrder: String?): Cursor {
        val locationSetting = WeatherEntry.getLocationSettingFromUri(uri)
        val startDate = WeatherEntry.getStartDateFromUri(uri)

        val selectionArgs: Array<String>
        val selection: String

        if (startDate == 0L) {
            selection = sLocationSettingSelection
            selectionArgs = arrayOf(locationSetting)
        } else {
            selectionArgs = arrayOf(locationSetting, java.lang.Long.toString(startDate))
            selection = sLocationSettingWithStartDateSelection
        }

        return sWeatherByLocationSettingQueryBuilder.query(read(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder)
    }

    private fun getWeatherByLocationSettingAndDate(uri: Uri, projection: Array<String>?, sortOrder: String?): Cursor {
        val locationSetting = WeatherEntry.getLocationSettingFromUri(uri)
        val date = WeatherEntry.getDateFromUri(uri)
        return sWeatherByLocationSettingQueryBuilder.query(read(),
                projection,
                sLocationSettingAndDaySelection,
                arrayOf(locationSetting, date.toString()),
                null,
                null,
                sortOrder)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        return when (uriMatcher.match(uri)) {
            WEATHER -> {
                normalizeDate(values ?: throw IllegalArgumentException("values required to insert weather"))
                val _id = write().insert(WeatherEntry.TABLE_NAME, null, values)
                if (_id > 0) WeatherEntry.buildWeatherUri(_id)
                else throw SQLException("Failed to insert row into " + uri)
            }
            LOCATION -> {
                val _id = write().insert(LocationEntry.TABLE_NAME, null, values)
                if (_id > 0) LocationEntry.buildLocationUri(_id)
                else throw SQLException("Failed to insert row into " + uri)
            }
            else -> throw UnsupportedOperationException("Unknown uri: " + uri)
        }.apply { notifyChange(uri) }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return when (uriMatcher.match(uri)) {
            WEATHER -> write().delete(WeatherEntry.TABLE_NAME, selection ?: "1", selectionArgs)
            LOCATION -> write().delete(LocationEntry.TABLE_NAME, selection ?: "1", selectionArgs)
            else -> throw UnsupportedOperationException("Unknown uri: " + uri)
        }.apply { notifyChange(uri) }
    }

    private fun normalizeDate(values: ContentValues) {
        if (values.containsKey(WeatherEntry.COLUMN_DATE)) {
            val dateValue = values.getAsLong(WeatherEntry.COLUMN_DATE)!!
            values.put(WeatherEntry.COLUMN_DATE, normalizeDate(dateValue).timeInMillis)
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return when (uriMatcher.match(uri)) {
            WEATHER -> {
                normalizeDate(values ?: throw IllegalArgumentException("values required to update weather"))
                write().update(WeatherEntry.TABLE_NAME, values, selection, selectionArgs)
            }
            LOCATION -> write().update(LocationEntry.TABLE_NAME, values, selection, selectionArgs)
            else -> throw UnsupportedOperationException("Unknown uri: " + uri)
        }.apply { notifyChange(uri) }
    }

    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        val db = write()
        val match = uriMatcher.match(uri)
        when (match) {
            WEATHER -> {
                db.beginTransaction()
                var returnCount = 0
                try {
                    for (value in values) {
                        normalizeDate(value)
                        val _id = db.insert(WeatherEntry.TABLE_NAME, null, value)
                        if (_id != -1L) {
                            returnCount++
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                notifyChange(uri)
                return returnCount
            }
            else -> return super.bulkInsert(uri, values)
        }
    }

    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @TargetApi(11)
    override fun shutdown() {
        mOpenHelper!!.close()
        super.shutdown()
    }

    private fun read() = mOpenHelper!!.readableDatabase

    private fun write() = mOpenHelper!!.writableDatabase

    private fun notifyChange(uri: Uri) = context!!.contentResolver.notifyChange(uri, null)

    companion object {
        val WEATHER = 100
        val WEATHER_WITH_LOCATION = 101
        val WEATHER_WITH_LOCATION_AND_DATE = 102
        val LOCATION = 300

        val LOC = LocationEntry.TABLE_NAME
        val WEA = WeatherEntry.TABLE_NAME

        val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.PATH_WEATHER, WEATHER)
            addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION)
            addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.PATH_WEATHER + "/*/#", WEATHER_WITH_LOCATION_AND_DATE)
            addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.PATH_LOCATION, LOCATION)
        }

        val sWeatherByLocationSettingQueryBuilder = SQLiteQueryBuilder().apply {
            tables = "$WEA INNER JOIN $LOC " + "ON $WEA.${WeatherEntry.COLUMN_LOC_KEY} = $LOC.${LocationEntry._ID}"
        }

        val sLocationSettingSelection = "$LOC.${LocationEntry.COLUMN_LOCATION_SETTING} = ?"

        val sLocationSettingWithStartDateSelection = "$LOC.${LocationEntry.COLUMN_LOCATION_SETTING} = ? AND ${WeatherEntry.COLUMN_DATE} >= ?"

        val sLocationSettingAndDaySelection = "$LOC.${LocationEntry.COLUMN_LOCATION_SETTING} = ? AND ${WeatherEntry.COLUMN_DATE} = ?"
    }
}