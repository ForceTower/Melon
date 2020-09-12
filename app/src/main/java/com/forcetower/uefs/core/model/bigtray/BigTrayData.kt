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

package com.forcetower.uefs.core.model.bigtray

import androidx.core.math.MathUtils.clamp
import com.forcetower.uefs.feature.shared.extensions.toCalendar
import timber.log.Timber
import java.util.Calendar
import java.util.Calendar.SATURDAY
import java.util.Calendar.SUNDAY
import java.util.Objects

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as BigTrayData? ?: return false
        return open == that.open &&
            quota == that.quota &&
            error == that.error &&
            type == that.type
    }

    override fun hashCode(): Int {
        return Objects.hash(open, quota, error, type, time)
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

    when (getNextMealType()) {
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

        return clamp(
            amount / when (type) {
                BigTrayData.LUNCH -> 1450
                BigTrayData.DINNER -> 490
                else -> 320
            },
            0f,
            1f
        ) * 100
    } catch (e: Exception) {
        Timber.d(e.message)
    }
    return 0.0f
}
