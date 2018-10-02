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

package com.forcetower.uefs.core.model.bigtray

import androidx.core.math.MathUtils.clamp
import com.forcetower.uefs.feature.shared.toCalendar
import timber.log.Timber
import java.lang.Exception
import java.util.*
import java.util.Calendar.SATURDAY
import java.util.Calendar.SUNDAY

data class BigTrayData(
    val open: Boolean,
    val quota: String,
    val error: Boolean,
    val time: Long,
    val type: String
) {
    companion object {
        const val COFFEE = 1
        const val LUNCH = 2
        const val DINNER = 3

        fun error() = BigTrayData(false, "", true, System.currentTimeMillis(), "")
        fun closed() = BigTrayData(false, "0", false, System.currentTimeMillis(), "")
        fun createData(values: List<String>) = BigTrayData(true, values[1].trim(), false, System.currentTimeMillis(), values[0].trim())
    }
}

fun BigTrayData.getNextMealType(): Int {
    val calendar = this.time.toCalendar()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minutes = calendar.get(Calendar.MINUTE)
    val account = hour * 60 + minutes

    return when {
        account < 9.5 * 60 -> BigTrayData.COFFEE
        account < 14.5 * 60 -> BigTrayData.LUNCH
        account < 20 * 60 -> BigTrayData.DINNER
        else -> BigTrayData.COFFEE
    }

}

fun BigTrayData.getPrice(): String {
    val type = getNextMealType()
    var amount = 0
    try {
        amount = this.quota.toInt()
    } catch (e: Exception) {}

    return when (type) {
        BigTrayData.COFFEE -> if (amount <= 0) "R$ 4,63" else "R$ 0,50"
        BigTrayData.LUNCH -> if (amount <= 0) "R$ 8,56" else "R$ 1,00"
        BigTrayData.DINNER -> if (amount <= 0) "R$ 3,94" else "R$ 0,70"
        else -> "R$ 1,00"
    }
}

fun BigTrayData.getNextMealTime(): String {
    val calendar = this.time.toCalendar()
    val day = calendar.get(Calendar.DAY_OF_WEEK)
    val type = getNextMealType()

    when (type) {
        BigTrayData.COFFEE -> return if (day == SUNDAY) "07h30min às 09h00min" else "06h30min às 09h00min"
        BigTrayData.LUNCH -> {
            if (day == SUNDAY) return "11h30min às 13h30min"
            return if (day == SATURDAY) "11h30min às 14h00min" else "10h30min às 14h00min"
        }
        else -> {
            if (day == SUNDAY) return "17h30min às 19h00min"
            return if (day == SATURDAY) "17h30min às 19h00min" else "17h30min às 19h30min"
        }
    }
}

fun BigTrayData.isOpen(): Boolean {
    var amount = -1
    try { amount = quota.toInt() } catch (e: Exception) {}
    return open && amount != -1
}

fun BigTrayData.percentage(): Float {
    try {
        val amount = quota.toFloat()
        val type = getNextMealType()

        return clamp(amount/when(type) {
            BigTrayData.LUNCH   -> 1450
            BigTrayData.DINNER  -> 490
            else -> 320
        }, 0f, 1f)
    } catch (e: Exception) {
        Timber.d(e.message)
    }
    return 0.0f
}