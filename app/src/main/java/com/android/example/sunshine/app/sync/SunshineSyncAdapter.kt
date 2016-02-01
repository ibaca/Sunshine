package com.android.example.sunshine.app.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.app.NotificationManager
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.*
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.util.Log
import com.android.example.sunshine.app.*
import com.android.example.sunshine.app.data.WeatherContract
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry.buildWeatherLocationAndDate
import org.json.JSONObject
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

class SunshineSyncAdapter(context: Context, autoInitialize: Boolean) : AbstractThreadedSyncAdapter(context, autoInitialize) {

    override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
        Log.d(LOG_TAG, "On perform sync...")
        val location = location(context)
        try {
            URL(uri(location)).readText().let { process(location, it) }
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "fetch and process location '$location' failure!, cause: $e", e)
        }
    }


    private fun uri(q: String) = "$base?q=$q&units=metric&cnt=$days&appid=$apiKey"

    private fun process(locationSetting: String, forecastJsonStr: String): Unit {
        val forecastJson = JSONObject(forecastJsonStr)
        val forecastArray = forecastJson.getJSONArray("list")

        val cityJson = forecastJson.getJSONObject("city")
        val cityName = cityJson.getString("name")

        val cityCoord = cityJson.getJSONObject("coord")
        val cityLatitude = cityCoord.getDouble("lat")
        val cityLongitude = cityCoord.getDouble("lon")

        val locationId = addLocation(context.contentResolver, locationSetting, cityName, cityLatitude, cityLongitude)

        val date = normalizeDate(); date.add(Calendar.DATE, -1)
        val forecastVector = (0..forecastArray.length() - 1).map {
            date.add(Calendar.DATE, 1)
            val dayForecast = forecastArray.getJSONObject(it)

            val pressure = dayForecast.getDouble("pressure")
            val humidity = dayForecast.getInt("humidity")
            val windSpeed = dayForecast.getDouble("speed")
            val windDirection = dayForecast.getDouble("deg")

            // description is in a child array called "weather", which is 1 element long.
            val weatherArray = dayForecast.getJSONArray("weather").getJSONObject(0)
            val description: String = weatherArray.getString("main")
            val weatherId = weatherArray.getString("id")
            val icon = weatherArray.getString("icon")

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            val temperatureObject = dayForecast.getJSONObject("temp")
            var high = temperatureObject.getDouble("max")
            var low = temperatureObject.getDouble("min")

            ContentValues().apply {
                Log.i(LOG_TAG, "inserting date ${date.time}")
                put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId)
                put(WeatherContract.WeatherEntry.COLUMN_DATE, date.timeInMillis)
                put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity)
                put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure)
                put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed)
                put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection)
                put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high)
                put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low)
                put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description)
                put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId)
                put(WeatherContract.WeatherEntry.COLUMN_ICON, icon)
            }
        }.toTypedArray()

        // add to database
        if (forecastVector.size > 0) {
            context.contentResolver.bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, forecastVector)

            context.contentResolver.delete(
                    WeatherContract.WeatherEntry.CONTENT_URI,
                    "${WeatherContract.WeatherEntry.COLUMN_DATE} <= ?",
                    arrayOf(normalizeDate().apply { add(Calendar.DATE, -1) }.timeInMillis.toString()));

            if (notifications(context)) {
                notifyWeather()
            }
        }

        Log.d(LOG_TAG, "FetchWeatherTask Complete. " + forecastVector.size + " inserted")
    }

    private fun notifyWeather() {
        val context = context
        //checking the last update and notify if it' the first of the day
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lastNotificationKey = context.getString(R.string.pref_last_notification)
        val lastSync = prefs.getLong(lastNotificationKey, 0)

        if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
            // Last sync was more than 1 day ago, let's send a notification with the weather.
            val locationQuery = location(context)
            val unit = units(context)

            val weatherUri = buildWeatherLocationAndDate(locationQuery, System.currentTimeMillis())

            // we'll query our contentProvider, as always
            context.contentResolver.query(weatherUri).use {
                if (!it.moveToFirst()) return


                val forecast = it.asContentValues().asForecast(unit)
                val title = context.getString(R.string.app_name)
                Log.i(LOG_TAG, "Showing notification $title - $forecast...")

                val contentText = context.getString(R.string.format_notification,
                        forecast.summary,
                        formatTemperature(context, forecast.max, unit),
                        formatTemperature(context, forecast.min, unit))

                val notification = NotificationCompat.Builder(getContext())
                        .setContentTitle(title).setContentText(contentText)

                // The stack builder object will contain an artificial back stack for the started Activity.
                // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
                notification.setContentIntent(TaskStackBuilder.create(context)
                        .addNextIntent(Intent(context, MainActivity::class.java))
                        .getPendingIntent(0, FLAG_UPDATE_CURRENT))

                // WEATHER_NOTIFICATION_ID allows you to update the notification later on.
                getContext().getSystemService(Context.NOTIFICATION_SERVICE)
                        .let { it as NotificationManager }
                        .notify(WEATHER_NOTIFICATION_ID, notification.build())

                //refreshing last sync
                val editor = prefs.edit()
                editor.putLong(lastNotificationKey, System.currentTimeMillis())
                editor.commit()
            }
        }
    }


    companion object {
        val LOG_TAG = SunshineSyncAdapter::class.java.simpleName
        val SYNC_INTERVAL: Int = TimeUnit.SECONDS.toSeconds(30).toInt();
        val SYNC_FLEXTIME: Int = SYNC_INTERVAL / 3;
        val DAY_IN_MILLIS = 1000 * 60 * 60 * 24.toLong()
        val WEATHER_NOTIFICATION_ID = 3004
        val apiKey = BuildConfig.OPENWEATHERMAP_APIKEY
        val base = "http://api.openweathermap.org/data/2.5/forecast/daily"
        val days = 14

        /** Helper method to have the sync adapter sync immediately */
        fun syncImmediately(context: Context) {
            val bundle = Bundle()
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            ContentResolver.requestSync(getSyncAccount(context),
                    context.getString(R.string.content_authority), bundle)
        }

        /**
         * Helper method to get the fake account to be used with SyncAdapter, or make a new one
         * if the fake account doesn't exist yet.  If we make a new account, we call the
         * onAccountCreated method so we can initialize things.
         */
        fun getSyncAccount(context: Context): Account? {
            val accountManager = context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager

            // Create the account type and default account
            val newAccount = Account(
                    context.getString(R.string.app_name),
                    context.getString(R.string.sync_account_type))

            // If the password doesn't exist, the account doesn't exist
            if (null == accountManager.getPassword(newAccount)) {
                Log.i(LOG_TAG, "Creating new account " + newAccount)

                /* Add the account and account type, no password or user data
                 If successful, return the Account object, otherwise report an error. */
                if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                    return null
                }

                onAccountCreated(newAccount, context)
            }
            return newAccount
        }

        fun onAccountCreated(newAccount: Account, context: Context) {
            Log.i(LOG_TAG, "New account created $newAccount, configuring periodic sync [$SYNC_INTERVAL, $SYNC_FLEXTIME]")

            /* Since we've created an account */
            SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME)

            /* Without calling setSyncAutomatically, our periodic sync will not be enabled. */
            ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true)

            /* Finally, let's do a sync to get things started */
            syncImmediately(context)
        }

        /** Helper method to schedule the sync adapter periodic execution */
        fun configurePeriodicSync(context: Context, syncInterval: Int, flexTime: Int) {
            val account = getSyncAccount(context)
            val authority = context.getString(R.string.content_authority)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // we can enable inexact timers in our periodic sync
                ContentResolver.requestSync(SyncRequest.Builder()
                        .syncPeriodic(syncInterval.toLong(), flexTime.toLong())
                        .setSyncAdapter(account, authority)
                        .setExtras(Bundle()).build())
            } else {
                ContentResolver.addPeriodicSync(account,
                        authority, Bundle(), syncInterval.toLong())
            }
        }

        fun initializeSyncAdapter(context: Context) = getSyncAccount(context)
    }
}