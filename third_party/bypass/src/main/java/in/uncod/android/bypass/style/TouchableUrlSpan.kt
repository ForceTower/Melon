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

package `in`.uncod.android.bypass.style

import android.content.res.ColorStateList
import android.text.TextPaint
import android.text.style.URLSpan

/**
 * An extension to URLSpan which changes it's background & foreground color when clicked.
 *
 * Derived from http://stackoverflow.com/a/20905824
 */
class TouchableUrlSpan(
    url: String,
    textColor: ColorStateList,
    private val pressedBackgroundColor: Int
) : URLSpan(url) {
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
