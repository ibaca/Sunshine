/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.example.sunshine.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import com.android.example.sunshine.app.data.WeatherContract.LocationEntry
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry


class WeatherDbHelper(context: Context) : SQLiteOpenHelper(context, WeatherDbHelper.DATABASE_NAME, null, WeatherDbHelper.DATABASE_VERSION) {

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {

        sqLiteDatabase.execSQL("CREATE TABLE ${LocationEntry.TABLE_NAME} (" +
                "${LocationEntry._ID} INTEGER PRIMARY KEY, " +
                "${LocationEntry.COLUMN_LOCATION_SETTING} TEXT UNIQUE NOT NULL, " +
                "${LocationEntry.COLUMN_CITY_NAME} TEXT NOT NULL, " +
                "${LocationEntry.COLUMN_COORD_LAT} REAL NOT NULL, " +
                "${LocationEntry.COLUMN_COORD_LON} REAL NOT NULL);")

        sqLiteDatabase.execSQL("CREATE TABLE ${WeatherEntry.TABLE_NAME} (" +
                "${WeatherEntry._ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "${WeatherEntry.COLUMN_LOC_KEY} INTEGER NOT NULL, " +
                "${WeatherEntry.COLUMN_DATE} INTEGER NOT NULL, " +
                "${WeatherEntry.COLUMN_SHORT_DESC} TEXT NOT NULL, " +
                "${WeatherEntry.COLUMN_WEATHER_ID} INTEGER NOT NULL," +
                "${WeatherEntry.COLUMN_MIN_TEMP} REAL NOT NULL, " +
                "${WeatherEntry.COLUMN_MAX_TEMP} REAL NOT NULL, " +
                "${WeatherEntry.COLUMN_HUMIDITY} REAL NOT NULL, " +
                "${WeatherEntry.COLUMN_PRESSURE} REAL NOT NULL, " +
                "${WeatherEntry.COLUMN_WIND_SPEED} REAL NOT NULL, " +
                "${WeatherEntry.COLUMN_DEGREES} REAL NOT NULL, " +
                "FOREIGN KEY (${WeatherEntry.COLUMN_LOC_KEY}) REFERENCES ${LocationEntry.TABLE_NAME} (${LocationEntry._ID}), " +
                "UNIQUE (${WeatherEntry.COLUMN_DATE}, ${WeatherEntry.COLUMN_LOC_KEY}) ON CONFLICT REPLACE);")
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS ${LocationEntry.TABLE_NAME}")
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS ${WeatherEntry.TABLE_NAME}")
        onCreate(sqLiteDatabase)
    }

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "weather.db"
    }
}
