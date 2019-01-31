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

package com.forcetower.uefs.feature.mechcalculator

import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.AppExecutors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class MechCalcRepository @Inject constructor(
    private val executors: AppExecutors
) {
    private var desiredMean = 7.0

    private val _result = MutableLiveData<MechResult>()
    val result: LiveData<MechResult>
        get() = _result

    private val values = mutableListOf<MechValue>()
    private val _mechanics = MutableLiveData<List<MechValue>>()
    val mechanics: LiveData<List<MechValue>>
        get() = _mechanics

    @AnyThread
    fun onAddValue(value: MechValue) {
        values.add(value)
        _mechanics.postValue(values)
        calculate()
    }

    @AnyThread
    fun onDeleteValue(value: MechValue) {
        values.remove(value)
        _mechanics.postValue(values)
        calculate()
    }

    @AnyThread
    fun calculate() {
        executors.others().execute {
            var wildcardWeight = 0.0
            var gradesSum = 0.0
            var weightSum = 0.0
            for (value in values) {
                val grade = value.grade
                if (grade == null) {
                    wildcardWeight += value.weight
                } else {
                    gradesSum += grade
                    weightSum += value.weight
                }
            }

            val rightEquation = (weightSum + wildcardWeight) * desiredMean
            if (wildcardWeight == 0.0) {
                val mean = gradesSum / weightSum
                if (gradesSum >= rightEquation) {
                    _result.postValue(MechResult(mean))
                } else {
                    val mech = onFinals(mean, false)
                    _result.postValue(mech)
                }
            } else {
                val newRight = rightEquation - gradesSum
                val wildcard = newRight / wildcardWeight
                val normWildcard = min(wildcard, 10.0)

                val additional = values.filter { it.grade == null }.sumByDouble { normWildcard * it.weight }
                val finalGrade = gradesSum + additional
                val finalWeight = weightSum + wildcardWeight
                val theMean = finalGrade / finalWeight
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
            val finalGrade = 12.5 - (1.5 * mean)
            if (finalGrade <= 8) {
                MechResult(mean, wildcard, finalGrade, final = true, lost = false)
            } else {
                MechResult(mean, wildcard, finalGrade, final = true, lost = true)
            }
        }
    }
}
