package com.android.example.sunshine.app.sync

import android.app.Service
import android.content.Intent
import android.util.Log

class SunshineSyncService : Service() {

    override fun onCreate() {
        Log.d("SunshineSyncService", "onCreate - SunshineSyncService")
        synchronized (syncAdapterLock) {
            if (sunshineSyncAdapter == null) {
                sunshineSyncAdapter = SunshineSyncAdapter(applicationContext, true)
            }
        }
    }

    override fun onBind(intent: Intent) = sunshineSyncAdapter!!.syncAdapterBinder

    companion object {
        private val syncAdapterLock = Object()
        private var sunshineSyncAdapter: SunshineSyncAdapter? = null
    }
}