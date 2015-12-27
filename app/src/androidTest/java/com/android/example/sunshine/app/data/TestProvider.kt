package com.android.example.sunshine.app.data

import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.test.AndroidTestCase
import android.util.Log
import com.android.example.sunshine.app.TEST_DATE
import com.android.example.sunshine.app.TEST_LOCATION
import com.android.example.sunshine.app.createNorthPoleLocationValues
import com.android.example.sunshine.app.createWeatherValues
import com.android.example.sunshine.app.data.WeatherContract.LocationEntry
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry.buildWeatherLocation
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry.buildWeatherLocationAndDate
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate
import com.android.example.sunshine.app.insertNorthPoleLocationValues
import com.android.example.sunshine.app.query
import com.android.example.sunshine.app.testContentObserver
import com.android.example.sunshine.app.validateCurrentRecord
import com.android.example.sunshine.app.validateCursor
import junit.framework.Assert


class TestProvider : AndroidTestCase() {

    fun deleteAllRecords() {
        mContext.contentResolver.delete(WeatherEntry.CONTENT_URI, null, null)
        mContext.contentResolver.delete(LocationEntry.CONTENT_URI, null, null)

        var cursor: Cursor = mContext.contentResolver.query(WeatherEntry.CONTENT_URI)
        Assert.assertEquals("Error: Records not deleted from Weather table during delete", 0, cursor.count)
        cursor.close()

        cursor = mContext.contentResolver.query(LocationEntry.CONTENT_URI)
        Assert.assertEquals("Error: Records not deleted from Location table during delete", 0, cursor.count)
        cursor.close()
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        deleteAllRecords()
    }

    fun testProviderRegistry() {
        val pm = mContext.packageManager
        val pkg = mContext.packageName

        val componentName = ComponentName(pkg, WeatherProvider::class.java.name)
        try {
            val expected = WeatherContract.CONTENT_AUTHORITY
            val actual = pm.getProviderInfo(componentName, 0).authority
            Assert.assertEquals("WeatherProvider authority mismatch", expected, actual)
        } catch (e: PackageManager.NameNotFoundException) {
            Assert.assertTrue("WeatherProvider should be registered at " + pkg, false)
        }
    }

    fun testGetType() {
        // content://com.example.android.sunshine.app/weather/
        var type: String = mContext.contentResolver.getType(WeatherEntry.CONTENT_URI)
        // vnd.android.cursor.dir/com.example.android.sunshine.app/weather
        Assert.assertEquals("Error: the WeatherEntry CONTENT_URI should return WeatherEntry.CONTENT_TYPE",
                WeatherEntry.CONTENT_TYPE, type)

        val testLocation = "94074"
        // content://com.example.android.sunshine.app/weather/94074
        type = mContext.contentResolver.getType(
                buildWeatherLocation(testLocation))
        // vnd.android.cursor.dir/com.example.android.sunshine.app/weather
        Assert.assertEquals("Error: the WeatherEntry CONTENT_URI with location should return WeatherEntry.CONTENT_TYPE",
                WeatherEntry.CONTENT_TYPE, type)

        val testDate = 1419120000L // December 21st, 2014
        // content://com.example.android.sunshine.app/weather/94074/20140612
        type = mContext.contentResolver.getType(
                buildWeatherLocationAndDate(testLocation, testDate))
        // vnd.android.cursor.item/com.example.android.sunshine.app/weather/1419120000
        Assert.assertEquals("Error: the WeatherEntry CONTENT_URI with location and date should return WeatherEntry.CONTENT_ITEM_TYPE",
                WeatherEntry.CONTENT_ITEM_TYPE, type)

        // content://com.example.android.sunshine.app/location/
        type = mContext.contentResolver.getType(LocationEntry.CONTENT_URI)
        // vnd.android.cursor.dir/com.example.android.sunshine.app/location
        Assert.assertEquals("Error: the LocationEntry CONTENT_URI should return LocationEntry.CONTENT_TYPE",
                LocationEntry.CONTENT_TYPE, type)
    }


    /** This test uses the database directly to insert and then uses the ContentProvider to read out the data. */
    fun testBasicWeatherQuery() {
        // insert our test records into the database
        val dbHelper = WeatherDbHelper(mContext)
        val db = dbHelper.writableDatabase

        val testValues = createNorthPoleLocationValues()
        val locationRowId = insertNorthPoleLocationValues(mContext)

        // Fantastic.  Now that we have a location, add some weather!
        val weatherValues = createWeatherValues(locationRowId)

        val weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues).toInt()
        Assert.assertTrue("Unable to Insert WeatherEntry into the Database", weatherRowId != -1)

        db.close()

        // Test the basic content provider query
        val weatherCursor = mContext.contentResolver.query(WeatherEntry.CONTENT_URI)

        // Make sure we get the correct cursor out of the database
        validateCursor(weatherCursor, weatherValues)
    }

    /** This test uses the database directly to insert and then uses the ContentProvider to read out the data. */
    fun testBasicLocationQueries() {
        // insert our test records into the database
        val dbHelper = WeatherDbHelper(mContext)
        val db = dbHelper.writableDatabase

        val testValues = createNorthPoleLocationValues()
        val locationRowId = insertNorthPoleLocationValues(mContext)

        // Test the basic content provider query
        val locationCursor = mContext.contentResolver.query(LocationEntry.CONTENT_URI)

        // Make sure we get the correct cursor out of the database
        validateCursor(locationCursor, testValues)

        // Has the NotificationUri been set correctly? --- we can only test this easily against API
        // level 19 or greater because getNotificationUri was added in API level 19.
        if (Build.VERSION.SDK_INT >= 19) {
            Assert.assertEquals("Error: Location Query did not properly set NotificationUri",
                    locationCursor.notificationUri, LocationEntry.CONTENT_URI)
        }
    }

    /** This test uses the provider to insert and then update the data. */
    fun testUpdateLocation() {
        // Create a new map of values, where column names are the keys
        val values = createNorthPoleLocationValues()

        val locationUri = mContext.contentResolver.insert(LocationEntry.CONTENT_URI, values)
        val locationRowId = ContentUris.parseId(locationUri).toInt()

        // Verify we got a row back.
        Assert.assertTrue(locationRowId != -1)
        Log.d(LOG_TAG, "New row id: " + locationRowId)

        val updatedValues = ContentValues(values)
        updatedValues.put(LocationEntry._ID, locationRowId)
        updatedValues.put(LocationEntry.COLUMN_CITY_NAME, "Santa's Village")

        // Create a cursor with observer to make sure that the content provider is notifying
        // the observers as expected
        val locationCursor = mContext.contentResolver.query(LocationEntry.CONTENT_URI)

        val tco = testContentObserver()
        locationCursor.registerContentObserver(tco)

        val count = mContext.contentResolver.update(
                LocationEntry.CONTENT_URI, updatedValues, LocationEntry._ID + "= ?",
                arrayOf(locationRowId.toString()))
        Assert.assertEquals(count, 1)

        // Test to make sure our observer is called.  If not, we throw an assertion.
        tco.waitForNotificationOrFail()

        locationCursor.unregisterContentObserver(tco)
        locationCursor.close()

        // A cursor is your primary interface to the query results.
        val cursor = mContext.contentResolver.query(
                LocationEntry.CONTENT_URI,
                null, // projection
                LocationEntry._ID + " = " + locationRowId,
                null, // Values for the "where" clause
                null    // sort order
        )

        validateCursor(cursor, updatedValues)

        cursor.close()
    }


    /** Make sure we can still delete after adding/updating stuff. */
    fun testInsertReadProvider() {
        val testValues = createNorthPoleLocationValues()

        // Register a content observer for our insert.  This time, directly with the content resolver
        var tco = testContentObserver()
        mContext.contentResolver.registerContentObserver(LocationEntry.CONTENT_URI, true, tco)
        val locationUri = mContext.contentResolver.insert(LocationEntry.CONTENT_URI, testValues)

        // Did our content observer get called?
        tco.waitForNotificationOrFail()
        mContext.contentResolver.unregisterContentObserver(tco)

        val locationRowId = ContentUris.parseId(locationUri).toInt()

        // Verify we got a row back.
        Assert.assertTrue(locationRowId != -1)

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // A cursor is your primary interface to the query results.
        val cursor = mContext.contentResolver.query(LocationEntry.CONTENT_URI)
        validateCursor(cursor, testValues)

        // Fantastic.  Now that we have a location, add some weather!
        val weatherValues = createWeatherValues(locationRowId.toLong())
        // The TestContentObserver is a one-shot class
        tco = testContentObserver()

        mContext.contentResolver.registerContentObserver(WeatherEntry.CONTENT_URI, true, tco)

        val weatherInsertUri = mContext.contentResolver.insert(WeatherEntry.CONTENT_URI, weatherValues)
        Assert.assertTrue(weatherInsertUri != null)

        tco.waitForNotificationOrFail()
        mContext.contentResolver.unregisterContentObserver(tco)

        // A cursor is your primary interface to the query results.
        var weatherCursor: Cursor = mContext.contentResolver.query(WeatherEntry.CONTENT_URI)

        validateCursor(weatherCursor, weatherValues)

        // Add the location values in with the weather data so that we can make
        // sure that the join worked and we actually get all the values back
        weatherValues.putAll(testValues)

        // Get the joined Weather and Location data
        weatherCursor = mContext.contentResolver.query(buildWeatherLocation(TEST_LOCATION))
        validateCursor(weatherCursor, weatherValues)

        // Get the joined Weather and Location data with a start date
        weatherCursor = mContext.contentResolver.query(buildWeatherLocationWithStartDate(TEST_LOCATION, TEST_DATE))
        validateCursor(weatherCursor, weatherValues)

        // Get the joined Weather data for a specific date
        weatherCursor = mContext.contentResolver.query(buildWeatherLocationAndDate(TEST_LOCATION, TEST_DATE))
        validateCursor(weatherCursor, weatherValues)
    }

    fun testDeleteRecords() {
        testInsertReadProvider()

        // Register a content observer for our location delete.
        val locationObserver = testContentObserver()
        mContext.contentResolver.registerContentObserver(LocationEntry.CONTENT_URI, true, locationObserver)

        // Register a content observer for our weather delete.
        val weatherObserver = testContentObserver()
        mContext.contentResolver.registerContentObserver(WeatherEntry.CONTENT_URI, true, weatherObserver)

        deleteAllRecords()

        locationObserver.waitForNotificationOrFail()
        weatherObserver.waitForNotificationOrFail()

        mContext.contentResolver.unregisterContentObserver(locationObserver)
        mContext.contentResolver.unregisterContentObserver(weatherObserver)
    }

    fun testBulkInsert() {
        // first, let's create a location value
        val testValues = createNorthPoleLocationValues()
        val locationUri = mContext.contentResolver.insert(LocationEntry.CONTENT_URI, testValues)
        val locationRowId = ContentUris.parseId(locationUri).toInt()

        // Verify we got a row back.
        Assert.assertTrue(locationRowId != -1)

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // A cursor is your primary interface to the query results.
        var cursor: Cursor = mContext.contentResolver.query(LocationEntry.CONTENT_URI)

        validateCursor(cursor, testValues)

        // Now we can bulkInsert some weather.  In fact, we only implement BulkInsert for weather
        // entries.  With ContentProviders, you really only have to implement the features you
        // use, after all.
        val bulkInsertContentValues = createBulkInsertWeatherValues(locationRowId.toLong())

        // Register a content observer for our bulk insert.
        val weatherObserver = testContentObserver()
        mContext.contentResolver.registerContentObserver(WeatherEntry.CONTENT_URI, true, weatherObserver)

        val insertCount = mContext.contentResolver.bulkInsert(WeatherEntry.CONTENT_URI, bulkInsertContentValues)

        weatherObserver.waitForNotificationOrFail()
        mContext.contentResolver.unregisterContentObserver(weatherObserver)

        Assert.assertEquals(insertCount, BULK_INSERT_RECORDS_TO_INSERT)

        // A cursor is your primary interface to the query results.
        cursor = mContext.contentResolver.query(
                uri = WeatherEntry.CONTENT_URI,
                sortOrder = "${WeatherEntry.COLUMN_DATE} ASC"
        )

        // we should have as many records in the database as we've inserted
        Assert.assertEquals(cursor.count, BULK_INSERT_RECORDS_TO_INSERT)

        // and let's make sure they match the ones we created
        cursor.moveToFirst()
        var i = 0
        while (i < BULK_INSERT_RECORDS_TO_INSERT) {
            validateCurrentRecord(cursor, bulkInsertContentValues[i])
            i++
            cursor.moveToNext()
        }
        cursor.close()
    }

    companion object {
        val LOG_TAG = TestProvider::class.java.simpleName
        val BULK_INSERT_RECORDS_TO_INSERT = 10

        internal fun createBulkInsertWeatherValues(locationRowId: Long): Array<out ContentValues> {
            var currentTestDate = TEST_DATE
            val millisecondsInADay = 1000 * 60 * 60 * 24.toLong()
            val returnContentValues = arrayOfNulls<ContentValues>(BULK_INSERT_RECORDS_TO_INSERT)

            var i = 0
            while (i < BULK_INSERT_RECORDS_TO_INSERT) {
                val weatherValues = ContentValues()
                weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationRowId)
                weatherValues.put(WeatherEntry.COLUMN_DATE, currentTestDate)
                weatherValues.put(WeatherEntry.COLUMN_DEGREES, 1.1)
                weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, 1.2 + 0.01 * i.toFloat())
                weatherValues.put(WeatherEntry.COLUMN_PRESSURE, 1.3 - 0.01 * i.toFloat())
                weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, 75 + i)
                weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, 65 - i)
                weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, "Asteroids")
                weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, 5.5 + 0.2 * i.toFloat())
                weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, 321)
                weatherValues.put(WeatherEntry.COLUMN_ICON, "10d")
                returnContentValues[i] = weatherValues
                i++
                currentTestDate += millisecondsInADay
            }
            return returnContentValues.requireNoNulls();
        }
    }
}
