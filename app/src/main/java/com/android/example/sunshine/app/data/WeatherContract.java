package com.android.example.sunshine.app.data;

import android.provider.BaseColumns;

/** Defines table and column names for the weather database. */
public class WeatherContract {

    public static final class LocationEntry implements BaseColumns {
        public static final String TABLE_NAME = "location";
        /** Location setting string is what will be sent to openweathermap as the location query. */
        public static final String COLUMN_LOCATION_SETTING = "location_setting";
        /** Human readable location string, provided by the API (e.g. "Mountain View" vs 94043). */
        public static final String COLUMN_CITY_NAME = "city_name";
        /** Latitude as returned by openweathermap. */
        public static final String COLUMN_COORD_LAT = "coord_lat";
        /** Longitude as returned by openweathermap. */
        public static final String COLUMN_COORD_LON = "coord_long";

    }

    public static final class WeatherEntry implements BaseColumns {
        public static final String TABLE_NAME = "weather";
        /** Column with the foreign key into the location table. */
        public static final String COLUMN_LOC_KEY = "location_id";
        /* Date, stored as long in milliseconds since the epoch and truncated at 00:00 UTC. */
        public static final String COLUMN_DATE = "date";
        /** Weather id as returned by API, to identify the icon to be used */
        public static final String COLUMN_WEATHER_ID = "weather_id";
        /** Short description of the weather, as provided by API. e.g "clear" vs "sky is clear". */
        public static final String COLUMN_SHORT_DESC = "short_desc";
        /** Min temperatures for the day (stored as floats) */
        public static final String COLUMN_MIN_TEMP = "min";
        /** Max temperatures for the day (stored as floats) */
        public static final String COLUMN_MAX_TEMP = "max";
        /** Humidity is stored as a float representing percentage. */
        public static final String COLUMN_HUMIDITY = "humidity";
        /** Humidity is stored as a float representing percentage. */
        public static final String COLUMN_PRESSURE = "pressure";
        /** Wind speed is stored as a float representing wind speed mph. */
        public static final String COLUMN_WIND_SPEED = "wind";
        /** Degrees are meteorological degrees (e.g, 0 is north, 180 is south) stored as floats. */
        public static final String COLUMN_DEGREES = "degrees";
    }
}
