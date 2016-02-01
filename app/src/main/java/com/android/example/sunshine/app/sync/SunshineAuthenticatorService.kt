package com.android.example.sunshine.app.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** The service which allows the sync adapter framework to access the authenticator. */
class SunshineAuthenticatorService : Service() {
    lateinit var authenticator: SunshineAuthenticator

    override fun onCreate() = SunshineAuthenticator(this).let { authenticator = it }

    override fun onBind(intent: Intent): IBinder = authenticator.iBinder
}
