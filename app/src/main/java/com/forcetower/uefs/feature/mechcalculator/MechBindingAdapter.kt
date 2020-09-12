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
