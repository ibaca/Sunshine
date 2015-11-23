package com.android.example.sunshine.app.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import static com.android.example.sunshine.app.UtilKt.normalizeDate;

/** Defines table and column names for the weather database. */
public class WeatherContract {

    public static final String CONTENT_AUTHORITY = "com.android.example.sunshine.app";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_WEATHER = "weather";
    public static final String PATH_LOCATION = "location";

    public static final class LocationEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;

        public static Uri buildLocationUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

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

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_WEATHER).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;

        public static Uri buildWeatherUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        /* Student: Fill in this buildWeatherLocation function */
        public static Uri buildWeatherLocation(String locationSetting) {
            return CONTENT_URI.buildUpon()
                    .appendPath(locationSetting)
                    .build();
        }

        public static Uri buildWeatherLocationWithStartDate(String locationSetting, long startDate) {
            String normalizedStartDate = Long.toString(normalizeDate(startDate).getTimeInMillis());
            return CONTENT_URI.buildUpon()
                    .appendPath(locationSetting)
                    .appendQueryParameter(COLUMN_DATE, normalizedStartDate)
                    .build();
        }

        public static Uri buildWeatherLocationWithDate(String locationSetting, long date) {
            return CONTENT_URI.buildUpon()
                    .appendPath(locationSetting)
                    .appendPath(Long.toString(normalizeDate(date).getTimeInMillis()))
                    .build();
        }

        public static String getLocationSettingFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static long getDateFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }

        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (null != dateString && dateString.length() > 0) return Long.parseLong(dateString);
            else return 0;
        }

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
        /** Weather icon as returned by API, the actual icon to be used. */
        public static final String COLUMN_ICON = "icon";
    }
}
