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

package com.forcetower.core.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

val Context.isDarkTheme
    get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

val View.windowInsetsControllerCompat: WindowInsetsControllerCompat?
    get() {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) {
                val window = ctx.window
                return window?.let { WindowCompat.getInsetsController(window, this) }
            }
            ctx = ctx.baseContext
        }
        return null
    }

fun View.closeKeyboard() {
    windowInsetsControllerCompat?.hide(WindowInsetsCompat.Type.ime())
}

fun View.openKeyboard() {
    windowInsetsControllerCompat?.show(WindowInsetsCompat.Type.ime())
}
