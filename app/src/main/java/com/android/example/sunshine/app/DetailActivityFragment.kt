package com.android.example.sunshine.app

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.ShareActionProvider
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.bindView
import rx.Observable.combineLatest
import rx.lang.kotlin.BehaviourSubject
import rx.subscriptions.CompositeSubscription

class DetailActivityFragment : Fragment() {
    val detail: TextView by bindView(R.id.detail_text)
    val forecast = BehaviourSubject<MainActivityFragment.Forecast>()
    val share = BehaviourSubject<ShareActionProvider>()
    val onResumeTracker = CompositeSubscription()

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_detail_fragment, menu)

        menu.findItem(R.id.action_share)
                ?.let { MenuItemCompat.getActionProvider(it) as? ShareActionProvider }
                ?.let { share.onNext(it) }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loaderManager.initLoader(0, null, object : LoaderManager.LoaderCallbacks<Cursor> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> = loader(data())
            override fun onLoadFinished(l: Loader<Cursor>, d: Cursor) = forecast.onNext(d.forecast())
            override fun onLoaderReset(l: Loader<Cursor>) = Unit
        })
    }

    override fun onResume() {
        super.onResume()
        val forecast = forecast.doOnNext({ detail.text = it.summary })
        onResumeTracker.subscribe(combineLatest(forecast, share) { i, s ->
            s.setShareIntent(i.asIntent())
        })
    }

    override fun onPause() {
        super.onPause()
        onResumeTracker.clear()
    }

    private fun data() = activity.intent?.data ?: throw IllegalStateException("data uri required")

    private fun loader(uri: Uri) = CursorLoader(context, uri, null, null, null, null)

    private fun Cursor.forecast() = first().asContentValues().asForecast()

    private fun ContentValues.asForecast() = asForecast(units(context))

    private fun MainActivityFragment.Forecast.asIntent(): Intent {
        return Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "$summary #SunshineApp")
    }
}
