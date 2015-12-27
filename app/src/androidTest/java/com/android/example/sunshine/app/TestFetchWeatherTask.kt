package com.android.example.sunshine.app

import android.annotation.TargetApi
import android.test.AndroidTestCase
import com.android.example.sunshine.app.data.WeatherContract.LocationEntry

class TestFetchWeatherTask : AndroidTestCase() {

    @TargetApi(11)
    fun testAddLocation() {
        // start from a clean state
        context.contentResolver.delete(LocationEntry.CONTENT_URI,
                LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                arrayOf(ADD_LOCATION_SETTING))

        val locationId = addLocation(context,
                ADD_LOCATION_SETTING, ADD_LOCATION_CITY, ADD_LOCATION_LAT, ADD_LOCATION_LON)

        // does addLocation return a valid record ID?
        assertFalse("Error: addLocation returned an invalid ID on insert", locationId == -1L)

        // test all this twice
        for (i in 0..1) {

            // does the ID point to our location?
            val locationCursor = context.contentResolver.query(
                    uri = LocationEntry.CONTENT_URI,
                    projection = arrayOf(
                            LocationEntry._ID,
                            LocationEntry.COLUMN_LOCATION_SETTING,
                            LocationEntry.COLUMN_CITY_NAME,
                            LocationEntry.COLUMN_COORD_LAT,
                            LocationEntry.COLUMN_COORD_LON),
                    selection = "${LocationEntry.COLUMN_LOCATION_SETTING} = ?",
                    selectionArgs = arrayOf(ADD_LOCATION_SETTING)
            )

            // these match the indices of the projection
            if (locationCursor.moveToFirst()) {
                assertEquals("Error: the queried value of locationId does not match the returned valuefrom addLocation", locationCursor.getLong(0), locationId)
                assertEquals("Error: the queried value of location setting is incorrect", locationCursor.getString(1), ADD_LOCATION_SETTING)
                assertEquals("Error: the queried value of location city is incorrect", locationCursor.getString(2), ADD_LOCATION_CITY)
                assertEquals("Error: the queried value of latitude is incorrect", locationCursor.getDouble(3), ADD_LOCATION_LAT)
                assertEquals("Error: the queried value of longitude is incorrect", locationCursor.getDouble(4), ADD_LOCATION_LON)
            } else {
                fail("Error: the id you used to query returned an empty cursor")
            }

            // there should be no more records
            assertFalse("Error: there should be only one record returned from a location query",
                    locationCursor.moveToNext())

            // add the location again
            val newLocationId = addLocation(context,
                    ADD_LOCATION_SETTING, ADD_LOCATION_CITY, ADD_LOCATION_LAT, ADD_LOCATION_LON)

            assertEquals("Error: inserting a location again should return the same ID",
                    locationId, newLocationId)
        }
        // reset our state back to normal
        context.contentResolver.delete(LocationEntry.CONTENT_URI,
                LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                arrayOf(ADD_LOCATION_SETTING))

        // clean up the test so that other tests can use the content provider
        context.contentResolver.acquireContentProviderClient(LocationEntry.CONTENT_URI)!!.localContentProvider!!.shutdown()
    }

    companion object {
        val ADD_LOCATION_SETTING = "Sunnydale, CA"
        val ADD_LOCATION_CITY = "Sunnydale"
        val ADD_LOCATION_LAT = 34.425833
        val ADD_LOCATION_LON = -119.714167
    }
}
