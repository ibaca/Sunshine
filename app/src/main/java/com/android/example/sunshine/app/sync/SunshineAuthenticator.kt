package com.android.example.sunshine.app.sync

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.content.Context
import android.os.Bundle

class SunshineAuthenticator(context: Context) : AbstractAccountAuthenticator(context) {

    // No properties to edit
    override fun editProperties(
            r: AccountAuthenticatorResponse,
            s: String) = throw UnsupportedOperationException()

    // Because we're not actually adding an account to the device, just return null.
    override fun addAccount(
            r: AccountAuthenticatorResponse,
            s: String,
            s2: String,
            strings: Array<String>,
            bundle: Bundle) = null

    // Ignore attempts to confirm credentials
    override fun confirmCredentials(
            r: AccountAuthenticatorResponse,
            account: Account,
            bundle: Bundle) = null

    // Getting an authentication token is not supported
    override fun getAuthToken(
            r: AccountAuthenticatorResponse,
            account: Account,
            s: String,
            bundle: Bundle) = throw UnsupportedOperationException()

    // Getting a label for the auth token is not supported
    override fun getAuthTokenLabel(s: String) = throw UnsupportedOperationException()

    // Updating user credentials is not supported
    override fun updateCredentials(
            r: AccountAuthenticatorResponse,
            account: Account,
            s: String, bundle: Bundle) = throw UnsupportedOperationException()

    // Checking features for the account is not supported
    override fun hasFeatures(
            r: AccountAuthenticatorResponse,
            account: Account,
            strings: Array<String>) = throw UnsupportedOperationException()
}
