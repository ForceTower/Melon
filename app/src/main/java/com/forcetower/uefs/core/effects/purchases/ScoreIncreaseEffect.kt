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

package com.forcetower.uefs.core.effects.purchases

import android.content.SharedPreferences
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

class ScoreIncreaseEffect @Inject constructor(
    private val preferences: SharedPreferences
) : PurchaseEffect {
    override fun runEffect() {
        Timber.d("Purchased score increase common")
        var currentIncrease = preferences.getFloat("score_increase_value", 0f)
        val currentExpire = preferences.getLong("score_increase_expires", -1)

        val now = Calendar.getInstance().timeInMillis
        if (currentExpire < now) currentIncrease = 0.0f

        val expires = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        preferences.edit()
            .putFloat("score_increase_value", currentIncrease + 0.2f)
            .putLong("score_increase_expires", expires)
            .apply()
    }
}
