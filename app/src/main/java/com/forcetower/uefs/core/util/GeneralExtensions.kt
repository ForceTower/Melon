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

package com.forcetower.uefs.core.util

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.forcetower.uefs.core.constants.Constants.SELECTED_INSTITUTION_KEY
import com.google.gson.Gson
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.Subject
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

fun Any.toJson(): String {
    val gson = Gson()
    return gson.toJson(this)
}

inline fun <reified T> String.fromJson(): T {
    val gson = Gson()
    return gson.fromJson(this, T::class.java)
}

fun Context.isConnectedToInternet(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE)
        as? ConnectivityManager ?: return false

    return manager.allNetworks.isNotEmpty()
}

fun Double.truncate(decimals: Int = 1): Double {
    val power = 10.0.pow(decimals.toDouble())
    return floor(this * power) / power
}

fun Double.round(decimals: Int = 1): Double {
    val power = 10.0.pow(decimals.toDouble())
    return (this * power).roundToInt() / power
}

fun SharedPreferences.isStudentFromUEFS(): Boolean {
    val inst = getString(SELECTED_INSTITUTION_KEY, "UEFS") ?: "UEFS"
    return inst == "UEFS"
}

fun SharedPreferences.isStudentFromUESC(): Boolean {
    val inst = getString(SELECTED_INSTITUTION_KEY, "UEFS") ?: "UEFS"
    return inst == "UESC"
}

fun <T> Subject<T>.toLiveData(): LiveData<T> {
    return LiveDataReactiveStreams.fromPublisher(this.toFlowable(BackpressureStrategy.LATEST))
}
