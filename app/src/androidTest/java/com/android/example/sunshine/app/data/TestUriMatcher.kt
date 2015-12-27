package com.android.example.sunshine.app.data

import android.test.AndroidTestCase
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry.buildWeatherLocation
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry.buildWeatherLocationAndDate

class TestUriMatcher : AndroidTestCase() {

    fun testUriMatcher() {
        val testMatcher = WeatherProvider.uriMatcher

        assertEquals("Error: The WEATHER URI was matched incorrectly.",
                WeatherProvider.WEATHER, testMatcher.match(TEST_WEATHER_DIR))
        assertEquals("Error: The WEATHER WITH LOCATION URI was matched incorrectly.",
                WeatherProvider.WEATHER_WITH_LOCATION, testMatcher.match(TEST_WEATHER_WITH_LOCATION_DIR))
        assertEquals("Error: The WEATHER WITH LOCATION AND DATE URI was matched incorrectly.",
                WeatherProvider.WEATHER_WITH_LOCATION_AND_DATE, testMatcher.match(TEST_WEATHER_WITH_LOCATION_AND_DATE_DIR))
        assertEquals("Error: The LOCATION URI was matched incorrectly.",
                WeatherProvider.LOCATION, testMatcher.match(TEST_LOCATION_DIR))
    }

    companion object {
        val LOCATION_QUERY = "London, UK"
        val TEST_DATE = 1419033600L  // December 20th, 2014
        val TEST_LOCATION_ID = 10L

        // content://com.example.android.sunshine.app/weather"
        val TEST_WEATHER_DIR = WeatherContract.WeatherEntry.CONTENT_URI
        val TEST_WEATHER_WITH_LOCATION_DIR = buildWeatherLocation(LOCATION_QUERY)
        val TEST_WEATHER_WITH_LOCATION_AND_DATE_DIR = buildWeatherLocationAndDate(LOCATION_QUERY, TEST_DATE)
        // content://com.example.android.sunshine.app/location"
        val TEST_LOCATION_DIR = WeatherContract.LocationEntry.CONTENT_URI
    }
}
