/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.unes.core.util

import `in`.uncod.android.bypass.style.TouchableUrlSpan
import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.method.MovementMethod
import android.view.MotionEvent
import android.widget.TextView

class LinkTouchMovementMethod : LinkMovementMethod() {
    private var pressedSpan: TouchableUrlSpan? = null

    override fun onTouchEvent(textView: TextView, spannable: Spannable, event: MotionEvent): Boolean {
        var handled = false
        if (event.action == MotionEvent.ACTION_DOWN) {
            pressedSpan = getPressedSpan(textView, spannable, event)
            if (pressedSpan != null) {
                pressedSpan!!.setPressed(true)
                Selection.setSelection(spannable, spannable.getSpanStart(pressedSpan),
                        spannable.getSpanEnd(pressedSpan))
                handled = true
            }
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            val touchedSpan = getPressedSpan(textView, spannable, event)
            if (pressedSpan != null && touchedSpan != pressedSpan) {
                pressedSpan!!.setPressed(false)
                pressedSpan = null
                Selection.removeSelection(spannable)
            }
        } else {
            if (pressedSpan != null) {
                pressedSpan!!.setPressed(false)
                super.onTouchEvent(textView, spannable, event)
                handled = true
            }
            pressedSpan = null
            Selection.removeSelection(spannable)
        }
        return handled
    }

    private fun getPressedSpan(textView: TextView, spannable: Spannable, event: MotionEvent): TouchableUrlSpan? {

        var x = event.x.toInt()
        var y = event.y.toInt()

        x -= textView.totalPaddingLeft
        y -= textView.totalPaddingTop

        x += textView.scrollX
        y += textView.scrollY

        val layout = textView.layout
        val line = layout.getLineForVertical(y)
        val off = layout.getOffsetForHorizontal(line, x.toFloat())

        val link = spannable.getSpans(off, off, TouchableUrlSpan::class.java)
        var touchedSpan: TouchableUrlSpan? = null
        if (link.isNotEmpty()) {
            touchedSpan = link[0]
        }
        return touchedSpan
    }

    companion object {


        private var instance: LinkTouchMovementMethod? = null

        fun getInstance(): MovementMethod {
            if (instance == null)
                instance = LinkTouchMovementMethod()

            return instance!!
        }
    }

}