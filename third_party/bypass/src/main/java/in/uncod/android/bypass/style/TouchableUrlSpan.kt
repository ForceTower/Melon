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

package `in`.uncod.android.bypass.style

import android.content.res.ColorStateList
import android.text.TextPaint
import android.text.style.URLSpan

/**
 * An extension to URLSpan which changes it's background & foreground color when clicked.
 *
 * Derived from http://stackoverflow.com/a/20905824
 */
class TouchableUrlSpan(url: String,
                       textColor: ColorStateList,
                       private val pressedBackgroundColor: Int) : URLSpan(url) {
    private var isPressed: Boolean = false
    private val normalTextColor: Int = textColor.defaultColor
    private val pressedTextColor: Int

    init {
        this.pressedTextColor = textColor.getColorForState(STATE_PRESSED, normalTextColor)
    }

    fun setPressed(isPressed: Boolean) {
        this.isPressed = isPressed
    }

    override fun updateDrawState(drawState: TextPaint) {
        drawState.color = if (isPressed) pressedTextColor else normalTextColor
        drawState.bgColor = if (isPressed) pressedBackgroundColor else 0
        drawState.isUnderlineText = !isPressed
    }

    companion object {

        private val STATE_PRESSED = intArrayOf(android.R.attr.state_pressed)
    }
}