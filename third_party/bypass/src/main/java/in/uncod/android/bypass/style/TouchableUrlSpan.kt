/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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