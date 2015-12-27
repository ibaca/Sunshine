package com.android.example.sunshine.app.data

import android.test.AndroidTestCase

class TestWeatherContract : AndroidTestCase() {

    fun testBuildWeatherLocation() {
        val locationUri = WeatherContract.WeatherEntry.buildWeatherLocation(TEST_WEATHER_LOCATION)
        assertNotNull("Null Uri returned.  You must fill-in buildWeatherLocation in WeatherContract.",
                locationUri)
        assertEquals("Weather location should be appended to the end of the Uri",
                TEST_WEATHER_LOCATION, locationUri.lastPathSegment)
        assertEquals("Error: Weather location Uri doesn't match our expected result",
                locationUri.toString(), "content://com.android.example.sunshine.app/weather/%2FNorth%20Pole")
    }

    companion object {
        // intentionally includes a slash to make sure Uri is getting quoted correctly
        val TEST_WEATHER_LOCATION = "/North Pole"
    }
}
