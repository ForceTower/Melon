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
import javax.inject.Inject

class MonkeyGoldEffect @Inject constructor(
    private val preferences: SharedPreferences
) : SubscriptionEffect {
    override fun runEffect() {
        preferences.edit().putBoolean("__monkey_gold_user_enabled__", true).apply()
    }

    override fun removeEffect() {
        preferences.edit().putBoolean("__monkey_gold_user_enabled__", false).apply()
    }

    override fun isEffectActive(): Boolean {
        return preferences.getBoolean("__monkey_gold_user_enabled__", false)
    }
}
