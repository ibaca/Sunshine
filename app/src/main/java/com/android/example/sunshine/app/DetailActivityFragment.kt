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
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import butterknife.bindView
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate
import com.android.example.sunshine.app.data.WeatherContract.WeatherEntry.getDateFromUri
import com.squareup.picasso.Picasso
import rx.Observable.combineLatest
import rx.lang.kotlin.BehaviourSubject
import rx.subscriptions.CompositeSubscription

class DetailActivityFragment : Fragment() {
    val detail: TextView by bindView(R.id.detail_day_textview)
    val image: ImageView by bindView(R.id.detail_icon)
    val forecast = BehaviourSubject<MainActivityFragment.Forecast>()
    val share = BehaviourSubject<ShareActionProvider>()
    val onResumeTracker = CompositeSubscription()
    val picasso by lazy { Picasso.with(context) }
    var uri: Uri? = null;

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
        uri = arguments?.getParcelable<Uri>(URI_KEY)
        Log.i(LOG_TAG, "Detail fragment created with dateUri $uri")
        uri?.let { loaderManager.initLoader(0, null, ForecastLoader(it)) }
    }

    override fun onResume() {
        super.onResume()
        val updater = forecast.doOnNext({
            detail.text = it.summary
            picasso.load(weatherIconUrl(it.iconUrl)).into(image)
            image.contentDescription = it.summary

        })
        onResumeTracker.subscribe(combineLatest(updater, share) { i, s ->
            s.setShareIntent(i.asIntent())
        })
    }

    override fun onPause() {
        super.onPause()
        onResumeTracker.clear()
    }

    fun onLocationChanged(location: String): Unit {
        Log.i(LOG_TAG, "Detail fragment notified of location changed $location")
        uri?.let {
            uri = buildWeatherLocationWithStartDate(location, getDateFromUri(it))
            loaderManager.restartLoader<Cursor>(0, null, ForecastLoader(uri!!))
        }
    }

    private fun loader(uri: Uri) = CursorLoader(context, uri, null, null, null, null)

    private fun Cursor.forecast() = first().asContentValues().asForecast()

    private fun ContentValues.asForecast() = asForecast(units(context))

    private fun MainActivityFragment.Forecast.asIntent(): Intent {
        return Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "$summary #SunshineApp")
    }

    inner class ForecastLoader(val dateUri: Uri) : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
            Log.i(LOG_TAG, "Create loader $dateUri")
            return loader(dateUri)
        }

        override fun onLoadFinished(l: Loader<Cursor>, d: Cursor) {
            Log.i(LOG_TAG, "Load finished $dateUri")
            forecast.onNext(d.forecast())
        }

        override fun onLoaderReset(l: Loader<Cursor>) = Unit
    }

    companion object {
        private val URI_KEY = "URI"

        fun of(dateUri: Uri) = DetailActivityFragment().apply {
            arguments = Bundle().apply { putParcelable(URI_KEY, dateUri) }
        }
    }
}
