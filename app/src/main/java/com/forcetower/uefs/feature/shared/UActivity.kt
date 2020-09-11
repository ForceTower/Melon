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

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import com.forcetower.uefs.feature.themeswitcher.ThemeOverlayUtils
import com.google.android.material.snackbar.Snackbar

abstract class UActivity : AppCompatActivity() {

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        if (shouldApplyThemeOverlay()) ThemeOverlayUtils.applyThemeOverlays(this)
        super.onCreate(savedInstanceState)
    }

    open fun showSnack(string: String, duration: Int = Snackbar.LENGTH_SHORT) {}

    open fun shouldApplyThemeOverlay() = true

    open fun getSnackInstance(string: String, duration: Int = Snackbar.LENGTH_SHORT): Snackbar? = null
}
