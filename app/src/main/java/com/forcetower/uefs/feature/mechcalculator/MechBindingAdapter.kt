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

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.uefs.R
import timber.log.Timber

@BindingAdapter("gradeOrWildcard")
fun gradeOrWildcard(tv: TextView, value: Double?) {
    if (value == null) {
        tv.text = "?"
    } else {
        tv.text = tv.context.getString(R.string.grade_format, value)
    }
}

@BindingAdapter("mechResult")
fun mechResult(tv: TextView, result: MechResult?) {
    val ctx = tv.context

    if (result == null) {
        tv.text = ctx.getString(R.string.mech_nothing_to_calculate)
        return
    }

    Timber.d("result: $result")
    val wildcard = result.wildcard
    val finalGrade = result.finalGrade
    val mean = result.mean

    if (result.lost) {
        tv.text = ctx.getText(R.string.mech_result_you_lost)
    } else if (finalGrade == null && wildcard == null) {
        tv.text = ctx.getString(R.string.mech_result_mean_only, mean)
    } else if (finalGrade != null && wildcard == null) {
        tv.text = ctx.getString(R.string.mech_result_value_in_final, mean, finalGrade)
    } else if (finalGrade == null && wildcard != null) {
        if (wildcard <= 0) {
            tv.text = ctx.getString(R.string.mech_result_wildcard_not_needed)
        } else {
            tv.text = ctx.getString(R.string.mech_result_wildcard_only, wildcard)
        }
    } else if (finalGrade != null && wildcard != null) {
        tv.text = ctx.getString(R.string.mech_result_wildcard_and_final, wildcard, finalGrade)
    }
}