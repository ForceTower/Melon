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

import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

abstract class UFragment : Fragment() {
    var displayName: String = javaClass.simpleName

    fun showSnack(string: String, duration: Int = Snackbar.LENGTH_SHORT) {
        val activity = activity
        if (activity is UActivity) {
            activity.showSnack(string, duration)
        } else {
            Timber.d("Not part of UActivity")
        }
    }

    fun getSnack(string: String, duration: Int = Snackbar.LENGTH_SHORT): Snackbar? {
        val activity = activity
        return if (activity is UActivity) {
            activity.getSnackInstance(string, duration)
        } else {
            Timber.d("Not part of UActivity")
            null
        }
    }
}
