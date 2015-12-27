package com.android.example.sunshine.app

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import com.android.example.sunshine.app.data.WeatherContract.LocationEntry
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry
import com.android.example.sunshine.app.data.WeatherDbHelper
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import junit.framework.Assert.fail
import java.util.concurrent.Callable

const val TEST_LOCATION = "99705"
const val TEST_DATE = 1419033600L  // December 20th, 2014

fun validateCursor(valueCursor: Cursor, expectedValues: ContentValues) {
    assertTrue("Cursor should not be empty", valueCursor.moveToFirst())
    validateCurrentRecord(valueCursor, expectedValues)
    valueCursor.close()
}

fun validateCurrentRecord(valueCursor: Cursor, expectedValues: ContentValues) {
    val valueSet = expectedValues.valueSet()
    for ((columnName, value) in valueSet) {
        val idx = valueCursor.getColumnIndex(columnName)
        assertFalse("Column '$columnName' not found", idx == -1)
        assertEquals(value.toString(), valueCursor.getString(idx))
    }
}

fun createWeatherValues(locationRowId: Long) = ContentValues().apply {
    put(WeatherEntry.COLUMN_LOC_KEY, locationRowId)
    put(WeatherEntry.COLUMN_DATE, TEST_DATE)
    put(WeatherEntry.COLUMN_DEGREES, 1.1)
    put(WeatherEntry.COLUMN_HUMIDITY, 1.2)
    put(WeatherEntry.COLUMN_PRESSURE, 1.3)
    put(WeatherEntry.COLUMN_MAX_TEMP, 75)
    put(WeatherEntry.COLUMN_MIN_TEMP, 65)
    put(WeatherEntry.COLUMN_SHORT_DESC, "Asteroids")
    put(WeatherEntry.COLUMN_WIND_SPEED, 5.5)
    put(WeatherEntry.COLUMN_WEATHER_ID, 321)
    put(WeatherEntry.COLUMN_ICON, "10d")
}

fun createNorthPoleLocationValues() = ContentValues().apply {
    put(LocationEntry.COLUMN_LOCATION_SETTING, TEST_LOCATION)
    put(LocationEntry.COLUMN_CITY_NAME, "North Pole")
    put(LocationEntry.COLUMN_COORD_LAT, 64.7488)
    put(LocationEntry.COLUMN_COORD_LON, -147.353)
}

fun insertNorthPoleLocationValues(context: Context): Long {
    // insert our test records into the database
    val dbHelper = WeatherDbHelper(context)
    val db = dbHelper.writableDatabase
    val testValues = createNorthPoleLocationValues()

    val locationRowId: Long
    locationRowId = db.insert(LocationEntry.TABLE_NAME, null, testValues)

    // Verify we got a row back.
    assertTrue("Failure to insert North Pole Location Values", locationRowId != -1L)

    return locationRowId
}

fun testContentObserver(): TestContentObserver {
    return TestContentObserver(HandlerThread("ContentObserverThread").apply { start() })
}

class TestContentObserver constructor(val mHT: HandlerThread) : ContentObserver(Handler(mHT.looper)) {
    var mContentChanged: Boolean = false

    // On earlier versions of Android, this onChange method is called
    override fun onChange(selfChange: Boolean) {
        onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        mContentChanged = true
    }

    fun waitForNotificationOrFail() {
        PollingCheck(5000, { mContentChanged }).run()
        mHT.quit()
    }
}

class PollingCheck(val mTimeout: Long = 3000L, val check: () -> Boolean) {

    fun run() {
        if (check()) return

        var timeout = mTimeout
        while (timeout > 0) {
            Thread.sleep(TIME_SLICE)
            if (check()) return
            timeout -= TIME_SLICE
        }

        fail("unexpected timeout")
    }

    companion object {
        const val TIME_SLICE = 50L

        fun check(message: CharSequence, timeout: Long, condition: Callable<Boolean>) {
            var t = timeout
            while (t > 0) {
                if (condition.call()) return
                Thread.sleep(TIME_SLICE)
                t -= TIME_SLICE
            }

            fail(message.toString())
        }
    }
}
