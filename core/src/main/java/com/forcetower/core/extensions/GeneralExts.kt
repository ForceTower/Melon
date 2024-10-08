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

import android.content.res.Resources
import androidx.annotation.DimenRes
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import kotlin.math.abs

fun Resources.getFloatUsingCompat(@DimenRes resId: Int): Float {
    return ResourcesCompat.getFloat(this, resId)
}

val Boolean?.orFalse
    get() = this ?: false

internal fun <T> MutableLiveData<T>.setValueIfNew(newValue: T) {
    if (this.value != newValue) value = newValue
}

fun Double.nearlyEquals(other: Double, difference: Double = 0.001): Boolean {
    return abs(this - other) <= difference
}
