@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("UtilKt")
package com.android.example.sunshine.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlin.reflect.KClass

public fun Activity.start(type: KClass<*>): Boolean {
    startActivity(Intent(this, type.java))
    return true
}

public fun Activity.start(action: String, uri: String): Boolean {
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

public fun <T> ArrayAdapter<T>.addIt(items: Iterable<T>) {
    setNotifyOnChange(false)
    clear()
    items.forEach { add(it) }
    setNotifyOnChange(true)
    notifyDataSetChanged()
}

fun location(c: Context): String = PreferenceManager.getDefaultSharedPreferences(c).getString(
        c.getString(R.string.pref_location_key), c.getString(R.string.pref_location_default))

fun units(c: Context): String = PreferenceManager.getDefaultSharedPreferences(c).getString(
        c.getString(R.string.pref_units_key), c.getString(R.string.pref_units_metric))
