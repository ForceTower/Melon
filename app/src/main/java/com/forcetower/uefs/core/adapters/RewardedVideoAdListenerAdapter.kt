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

package com.forcetower.uefs.core.adapters

import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAdListener

interface RewardedVideoAdListenerAdapter : RewardedVideoAdListener {
    override fun onRewardedVideoAdClosed() = Unit
    override fun onRewardedVideoAdLeftApplication() = Unit
    override fun onRewardedVideoAdLoaded() = Unit
    override fun onRewardedVideoAdOpened() = Unit
    override fun onRewardedVideoCompleted() = Unit
    override fun onRewarded(item: RewardItem?) = Unit
    override fun onRewardedVideoStarted() = Unit
    override fun onRewardedVideoAdFailedToLoad(reason: Int) = Unit
}
