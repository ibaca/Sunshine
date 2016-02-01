package com.android.example.sunshine.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import butterknife.bindOptionalView
import butterknife.bindView
import com.android.example.sunshine.app.sync.SunshineSyncAdapter

class MainActivity : AppCompatActivity(), MainActivityFragment.Callback {
    private val DETAIL_TAG = "detail"
    val toolbar: Toolbar by bindView(R.id.toolbar)
    val detail: View? by bindOptionalView(R.id.weather_detail_container)
    val twoPane: Boolean get() = detail != null;
    var location = "none"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar.apply { elevation = 0f })
        if (twoPane && savedInstanceState == null) supportFragmentManager.tx {
            replace(R.id.weather_detail_container, DetailActivityFragment(), DETAIL_TAG)
        }
        supportFragmentManager.findFragmentById(R.id.fragment_forecast)
                .let { it as MainActivityFragment }
                .useTodayLayout = !twoPane
        SunshineSyncAdapter.initializeSyncAdapter(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return menuInflater.inflate(R.menu.menu_main, menu).let { true }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_settings -> start(SettingsActivity::class)
        else -> super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        val newLocation = location(this)
        if (newLocation != location) {
            supportFragmentManager.findFragmentById(R.id.fragment_forecast)
                    .let { it as MainActivityFragment }
                    .onLocationChanged()
            supportFragmentManager.findFragmentByTag(DETAIL_TAG)
                    ?.let { it as? DetailActivityFragment }
                    ?.onLocationChanged(newLocation)
            location = newLocation
            Toast.makeText(this, "New location " + newLocation, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onItemSelected(dateUri: Uri): Unit {
        Log.i(LOG_TAG, "Main activity item selected $dateUri")
        when (twoPane) {
            true -> supportFragmentManager.tx {
                replace(R.id.weather_detail_container, DetailActivityFragment.of(dateUri))
            }
            false -> startActivity(Intent(this, DetailActivity::class.java).setData(dateUri))
        }
    }
}
