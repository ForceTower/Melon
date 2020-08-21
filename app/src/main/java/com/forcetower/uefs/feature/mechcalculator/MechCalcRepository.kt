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

package com.forcetower.uefs.feature.mechcalculator

import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.util.round
import com.forcetower.uefs.core.util.truncate
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class MechCalcRepository @Inject constructor(
    private val executors: AppExecutors
) {
    private var desiredMean = 7.0

    private val _result = MutableLiveData<MechResult?>()
    val result: LiveData<MechResult?>
        get() = _result

    private val values = mutableListOf<MechValue>()
    private val _mechanics = MutableLiveData<List<MechValue>>()
    val mechanics: LiveData<List<MechValue>>
        get() = _mechanics

    @AnyThread
    fun onAddValue(value: MechValue) {
        values.add(value)
        _mechanics.postValue(values.toMutableList())
        calculate()
    }

    @AnyThread
    fun onDeleteValue(value: MechValue) {
        values.remove(value)
        _mechanics.postValue(values.toMutableList())
        calculate()
    }

    @AnyThread
    fun calculate() {
        executors.others().execute {
            Timber.d("Values: $values")
            var wildcardWeight = 0.0
            var gradesSum = 0.0
            var weightSum = 0.0
            for (value in values) {
                val grade = value.grade
                if (grade == null) {
                    wildcardWeight += value.weight
                } else {
                    gradesSum += (grade * value.weight).truncate()
                    weightSum += value.weight
                }
            }

            val rightEquation = ((weightSum + wildcardWeight) * desiredMean).truncate()
            if (wildcardWeight == 0.0) {
                val mean = (gradesSum / weightSum).truncate()
                if (gradesSum >= rightEquation) {
                    _result.postValue(MechResult(mean))
                } else {
                    val mech = onFinals(mean, false)
                    _result.postValue(mech)
                }
            } else {
                val newRight = rightEquation - gradesSum
                val wildcard = (newRight / wildcardWeight).truncate()
                val normWildcard = min(wildcard, 10.0)

                val additional = values.filter { it.grade == null }.sumByDouble { (normWildcard * it.weight).truncate() }
                val finalGrade = gradesSum + additional
                val finalWeight = weightSum + wildcardWeight
                val theMean = (finalGrade / finalWeight).truncate()
                if (wildcard > 10) {
                    val mech = onFinals(theMean, true)
                    _result.postValue(mech)
                } else {
                    _result.postValue(MechResult(theMean, wildcard, null, final = false, lost = false))
                }
            }
        }
    }

    private fun onFinals(mean: Double, needsWildcard: Boolean): MechResult {
        val wildcard = if (needsWildcard) 10.0 else null
        return if (mean < 3) {
            MechResult(mean, null, null, final = false, lost = true)
        } else {
            val finalGrade = (12.5 - (1.5 * mean)).round()
            if (finalGrade <= 8) {
                MechResult(mean, wildcard, finalGrade, final = true, lost = false)
            } else {
                MechResult(mean, wildcard, finalGrade, final = true, lost = true)
            }
        }
    }
}
