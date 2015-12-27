package com.android.example.sunshine.app

import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager

class SettingsActivity : PreferenceActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_general)
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)))
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units_key)))
    }

    private fun bindPreferenceSummaryToValue(preference: Preference) {
        val pref = PreferenceManager.getDefaultSharedPreferences(preference.context)
        preference.setOnPreferenceChangeListener { p, v -> onPreferenceChange(p, v) }
        onPreferenceChange(preference, pref.getString(preference.key, ""))
    }

    fun onPreferenceChange(preference: Preference, value: Any): Boolean {
        val stringValue = value.toString()

        if (preference is ListPreference) {
            // Look up the correct display value in the preference's 'entries' list
            val prefIndex = preference.findIndexOfValue(stringValue)
            if (prefIndex >= 0) preference.summary = preference.entries[prefIndex]
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.summary = stringValue
        }
        return true
    }
}



