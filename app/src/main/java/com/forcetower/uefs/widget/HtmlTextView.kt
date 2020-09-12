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

package com.forcetower.uefs.widget

import android.content.Context
import android.text.Spanned
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.textview.MaterialTextView

class HtmlTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MaterialTextView(context, attrs) {

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) && text is Spanned) {
            var x = event.x.toInt()
            var y = event.y.toInt()

            x -= totalPaddingLeft
            y -= totalPaddingTop

            x += scrollX
            y += scrollY

            val layout = layout
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())

            val link = (text as Spanned).getSpans(off, off, ClickableSpan::class.java)
            if (link.isNotEmpty()) {
                if (action == MotionEvent.ACTION_UP) {
                    link[0].onClick(this)
                }
                return true
            }
        }
        return false
    }
}
