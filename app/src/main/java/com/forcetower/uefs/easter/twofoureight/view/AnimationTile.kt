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

package com.forcetower.uefs.easter.twofoureight.view

import kotlin.math.max

internal class AnimationTile(
    x: Int,
    y: Int,
    val animationType: Int,
    private val mAnimationTime: Long,
    private val mDelayTime: Long,
    val extras: IntArray?
) : Position(x, y) {
    private var mTimeElapsed: Long = 0

    val percentageDone: Double
        get() = max(0.0, 1.0 * (mTimeElapsed - mDelayTime) / mAnimationTime)

    val isActive: Boolean
        get() = mTimeElapsed >= mDelayTime

    fun tick(timeElapsed: Long) {
        this.mTimeElapsed = this.mTimeElapsed + timeElapsed
    }

    fun animationDone(): Boolean {
        return mAnimationTime + mDelayTime < mTimeElapsed
    }
}
