package com.android.example.sunshine.app

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.ShareActionProvider
import android.view.*
import android.widget.TextView
import butterknife.bindView

class DetailActivityFragment : Fragment() {
    val detail: TextView by bindView(R.id.detail_text)

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
                ?.setShareIntent(share())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        detail.text = data()
    }

    private fun data() = activity.intent?.extras?.getString(Intent.EXTRA_TEXT) ?: "No data"

    private fun share() = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "${data()} #SunshineApp")

}
