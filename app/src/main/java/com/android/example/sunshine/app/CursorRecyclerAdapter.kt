package com.android.example.sunshine.app

import android.database.Cursor
import android.support.v7.widget.RecyclerView

abstract class CursorRecyclerAdapter<VH : RecyclerView.ViewHolder>() : RecyclerView.Adapter<VH>() {
    protected var cursor: Cursor? = null
    protected var mRowIdColumn: Int = 0

    init {
        setHasStableIds(true)
    }

    final override fun onBindViewHolder(holder: VH, position: Int) = cursor.let { c ->
        check(c != null) { "this should only be called when the cursor is valid" }
        check(c!!.moveToPosition(position)) { "couldn't move cursor to position $position" }
        onBindViewHolder(holder, c)
    }

    abstract fun onBindViewHolder(holder: VH, cursor: Cursor)

    final override fun getItemCount(): Int = if (cursor != null) cursor!!.count else 0

    final override fun getItemId(position: Int): Long = cursor.let { c ->
        if (c != null && c.moveToPosition(position)) {
            return c.getLong(mRowIdColumn)
        } else {
            return RecyclerView.NO_ID
        }
    }

    fun changeCursor(cursor: Cursor) {
        swapCursor(cursor)?.close()
    }

    fun swapCursor(newCursor: Cursor?): Cursor? {
        if (newCursor === cursor) {
            return null
        }
        val oldCursor = cursor
        cursor = newCursor
        if (newCursor != null) {
            mRowIdColumn = newCursor.getColumnIndexOrThrow("_id")
            // notify the observers about the new cursor
            notifyDataSetChanged()
        } else {
            // notify the observers about the lack of a data set
            notifyItemRangeRemoved(0, itemCount)
        }
        return oldCursor
    }
}