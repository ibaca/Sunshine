package com.android.example.sunshine.app

import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.View

abstract class CursorRecyclerAdapter<VH : RecyclerView.ViewHolder>() : RecyclerView.Adapter<VH>() {
    var cursor: Cursor? = null
    var rowIdColumn: Int = 0

    init {
        setHasStableIds(true)
    }

    final override fun onBindViewHolder(holder: VH, position: Int) = cursor.let { c ->
        check(c != null) { "this should only be called when the cursor is valid" }
        check(c!!.moveToPosition(position)) { "couldn't move cursor to position $position" }

        // Set selected state; use a state list drawable to style the view
        holder.itemView.isSelected = focusedItem == position;

        onBindViewHolder(holder, c)
    }

    abstract fun onBindViewHolder(holder: VH, cursor: Cursor)

    final override fun getItemCount(): Int = if (cursor != null) cursor!!.count else 0

    final override fun getItemId(position: Int): Long = cursor.let { c ->
        if (c != null && c.moveToPosition(position)) {
            return c.getLong(rowIdColumn)
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
            rowIdColumn = newCursor.getColumnIndexOrThrow("_id")
            // notify the observers about the new cursor
            notifyDataSetChanged()
        } else {
            // notify the observers about the lack of a data set
            notifyItemRangeRemoved(0, itemCount)
        }
        return oldCursor
    }

    var focusedItem: Int = 0

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        super.onAttachedToRecyclerView(recyclerView)

        fun tryMoveSelection(lm: RecyclerView.LayoutManager, direction: Int): Boolean {
            var tryFocusItem = focusedItem + direction

            // If still within valid bounds, move the selection, notify to redraw, and scroll
            if (tryFocusItem >= 0 && tryFocusItem < itemCount) {
                updateFocusedItem(tryFocusItem)
                lm.scrollToPosition(focusedItem)
                return true;
            } else {
                return false;
            }
        }
        // Handle key up and key down and attempt to move selection
        recyclerView?.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->

            // Return false if scrolled to the bounds and allow focus to move off the list
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    return@OnKeyListener tryMoveSelection(recyclerView.layoutManager, 1);
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    return@OnKeyListener tryMoveSelection(recyclerView.layoutManager, -1);
                }
            }

            return@OnKeyListener false;
        });
    }

    fun updateFocusedItem(focusItem: Int) {
        focusedItem = focusItem
        notifyDataSetChanged()
    }
}