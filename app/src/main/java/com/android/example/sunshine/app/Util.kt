@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("UtilKt")
package com.android.example.sunshine.app

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.DatabaseUtils
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.preference.PreferenceManager
import android.support.v7.graphics.Palette
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.squareup.picasso.Callback
import com.squareup.picasso.RequestCreator
import java.util.*
import kotlin.reflect.KClass

const val LOG_TAG = "SUNSHINE"

fun Activity.start(type: KClass<*>): Boolean {
    startActivity(Intent(this, type.java))
    return true
}

fun Activity.start(action: String, uri: String): Boolean {
    try {
        val intent = Intent(action, Uri.parse(uri))
        if (intent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "Your android sucks!", Toast.LENGTH_LONG)
        } else {
            startActivity(intent)
        }
    } catch (e: Exception) {
        Toast.makeText(this, "Something goes wong!", Toast.LENGTH_LONG)
        Log.e(this.javaClass.simpleName, "Error starting Intent{action=$action, uri=$uri", e)
    }
    return true
}

fun Cursor.asContentValuesVector(): Vector<ContentValues> {
    var vector = Vector<ContentValues>(count)
    var populate: (ContentValues) -> Unit = { DatabaseUtils.cursorRowToContentValues(this, it) }
    if (moveToFirst()) {
        do {
            vector.add(ContentValues().apply { populate(this) })
        } while (moveToNext())
    }
    return vector;
}

fun RequestCreator.into(target: ImageView, callback: (Palette.Swatch) -> Unit) {
    into(target, object : Callback {
        override fun onSuccess() {
            Palette.from((target.drawable as BitmapDrawable).bitmap)
                    .generate({ callback.invoke(it.swatches[0]) })
        }

        override fun onError() {
            Log.e(LOG_TAG, "forecast icon load error")
        }
    })
}

fun Uri.contentId() = ContentUris.parseId(this)

fun location(c: Context): String = PreferenceManager.getDefaultSharedPreferences(c).getString(
        c.getString(R.string.pref_location_key), c.getString(R.string.pref_location_default))

fun units(c: Context): String = PreferenceManager.getDefaultSharedPreferences(c).getString(
        c.getString(R.string.pref_units_key), c.getString(R.string.pref_units_metric))

/** To make it easy to query for the exact date, normalize all database dates. */
fun normalizeDate(dateInMillis: Long = System.currentTimeMillis()) = GregorianCalendar(UTC).apply {
    timeInMillis = dateInMillis
    set(Calendar.HOUR, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

val UTC by lazy { TimeZone.getTimeZone("UTC") }
