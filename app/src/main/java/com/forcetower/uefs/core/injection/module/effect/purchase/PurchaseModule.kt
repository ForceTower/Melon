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

package com.forcetower.uefs.core.injection.module.effect.purchase

import android.content.SharedPreferences
import com.forcetower.uefs.core.effects.purchases.MonkeyGoldEffect
import com.forcetower.uefs.core.effects.purchases.PurchaseEffect
import com.forcetower.uefs.core.effects.purchases.ScoreIncreaseEffect
import com.forcetower.uefs.core.effects.purchases.SubscriptionEffect
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PurchaseModule {
    @Named("scoreIncreaseEffect")
    @Provides
    @Singleton
    fun scoreIncreaseEffect(preferences: SharedPreferences): PurchaseEffect {
        return ScoreIncreaseEffect(preferences)
    }

    @Named("monkeyGoldEffect")
    @Provides
    @Singleton
    fun monkeyGoldEffect(preferences: SharedPreferences): SubscriptionEffect {
        return MonkeyGoldEffect(preferences)
    }
}
