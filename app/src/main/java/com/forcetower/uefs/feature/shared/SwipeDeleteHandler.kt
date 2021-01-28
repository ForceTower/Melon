/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.feature.shared

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R

class SwipeDeleteHandler(
    context: Context,
    private val onDelete: (RecyclerView.ViewHolder) -> Unit,
    direction: Int = ItemTouchHelper.LEFT,
    private val ignored: List<Class<*>> = emptyList()
) : ItemTouchHelper.SimpleCallback(0, direction) {
    private val background = ColorDrawable(Color.WHITE)
    private val xMark = ContextCompat.getDrawable(context, R.drawable.ic_delete_black_24dp)?.apply {
        colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.BLACK, BlendModeCompat.SRC_ATOP)
    }
    private val xMarkMargin = context.resources.getDimension(R.dimen.delete_margin).toInt()

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onDelete(viewHolder)
    }

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return when (viewHolder::class.java) {
            in ignored -> 0
            else -> super.getSwipeDirs(recyclerView, viewHolder)
        }
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        if (viewHolder.adapterPosition < 0) return

        val view = viewHolder.itemView // the view being swiped

        background.apply {
            setBounds(view.right + dX.toInt(), view.top, view.right, view.bottom)
            draw(c)
        }

        // draw the symbol
        xMark?.apply {
            val xt = view.top + (view.bottom - view.top - xMark.intrinsicHeight) / 2
            setBounds(
                view.right - xMarkMargin - xMark.intrinsicWidth,
                xt,
                view.right - xMarkMargin,
                xt + xMark.intrinsicHeight
            )
            draw(c)
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
