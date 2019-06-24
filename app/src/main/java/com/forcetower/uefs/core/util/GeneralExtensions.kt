/*
 * Copyright (c) 2019.
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

package com.forcetower.uefs.core.util

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import com.forcetower.uefs.core.constants.Constants.SELECTED_INSTITUTION_KEY
import com.google.gson.Gson
import java.lang.Math.pow
import kotlin.math.floor
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
    val power = pow(10.0, decimals.toDouble())
    return floor(this * power) / power
}

fun Double.round(decimals: Int = 1): Double {
    val power = pow(10.0, decimals.toDouble())
    return (this * power).roundToInt() / power
}

fun SharedPreferences.isStudentFromUEFS(): Boolean {
    val inst = getString(SELECTED_INSTITUTION_KEY, "UEFS") ?: "UEFS"
    return inst == "UEFS"
}