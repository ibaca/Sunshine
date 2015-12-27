package com.android.example.sunshine.app

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import butterknife.bindView

class MainActivity : AppCompatActivity() {
    val toolbar: Toolbar by bindView(R.id.toolbar)
    var location = "none"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return menuInflater.inflate(R.menu.menu_main, menu).let { true }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_settings -> start(SettingsActivity::class)
        R.id.action_map -> start(Intent.ACTION_VIEW, "geo:0,0?q=${location(this)}")
        else -> super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        val newLocation = location(this)
        if (newLocation != location) {
            supportFragmentManager.findFragmentById(R.id.fragment)
                    .let({ it as MainActivityFragment })
                    .onLocationChanged()
            location = newLocation
            Toast.makeText(this, "New location " + newLocation, Snackbar.LENGTH_SHORT).show()
        }
    }
}
