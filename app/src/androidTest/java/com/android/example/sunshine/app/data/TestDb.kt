package com.android.example.sunshine.app.data

import android.test.AndroidTestCase
import com.android.example.sunshine.app.createNorthPoleLocationValues
import com.android.example.sunshine.app.createWeatherValues
import com.android.example.sunshine.app.data.WeatherContract.LocationEntry
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry
import com.android.example.sunshine.app.insertNorthPoleLocationValues
import com.android.example.sunshine.app.validateCursor

class TestDb : AndroidTestCase() {

    override fun setUp() {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME)
    }

    fun testCreateDb() = WeatherDbHelper(mContext).writableDatabase.apply {
        assertEquals(true, isOpen)

        // have we created the tables we want?
        rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).apply {
            assertTrue("Database has not been created correctly", moveToFirst())

            val tableNames = hashSetOf(LocationEntry.TABLE_NAME, WeatherEntry.TABLE_NAME)
            do tableNames.remove(getString(0)) while (moveToNext())

            assertTrue("Location and Weather tables should exists", tableNames.isEmpty())
        }.close()


        // now, do our tables contain the correct columns?
        rawQuery("PRAGMA table_info(" + LocationEntry.TABLE_NAME + ")", null).apply {
            assertTrue("Unable to query the database for table information", moveToFirst())

            val columns = hashSetOf(
                    LocationEntry._ID,
                    LocationEntry.COLUMN_CITY_NAME,
                    LocationEntry.COLUMN_COORD_LAT,
                    LocationEntry.COLUMN_COORD_LON,
                    LocationEntry.COLUMN_LOCATION_SETTING)

            val columnNameIndex = getColumnIndex("name")
            do columns.remove(getString(columnNameIndex)) while (moveToNext())

            assertTrue("Database should contain all location entry columns", columns.isEmpty())
        }.close()
    }.close()

    fun testLocationTable() {
        val db = WeatherDbHelper(mContext).writableDatabase
        val northPole = createNorthPoleLocationValues()

        val rowId = db.insert(LocationEntry.TABLE_NAME, null, northPole)
        assertTrue("Insert should succeed", rowId >= 0)

        val cursor = db.query(LocationEntry.TABLE_NAME, null, null, null, null, null, null)
        validateCursor(cursor, northPole)

        cursor.close()
    }

    fun testWeatherTable() {
        val db = WeatherDbHelper(mContext).writableDatabase
        val weatherValues = createWeatherValues(insertNorthPoleLocationValues(mContext))

        assertTrue("Insert should succeed", db.insert(WeatherEntry.TABLE_NAME, null, weatherValues) >= 0)
        val cursor = db.query(WeatherEntry.TABLE_NAME, null, null, null, null, null, null)
        validateCursor(cursor, weatherValues)
        cursor.close()
    }
}
