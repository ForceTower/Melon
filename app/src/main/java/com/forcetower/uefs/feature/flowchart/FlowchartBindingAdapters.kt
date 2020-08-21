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

package com.forcetower.uefs.feature.flowchart

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.sagres.utils.WordUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.FlowchartRequirementUI

@BindingAdapter("departmentStrip")
fun departmentStrip(tv: TextView, department: String?) {
    department ?: return
    val replaceOne = department.replace("departamento de ", "", ignoreCase = true)
    val replaceTwo = replaceOne.replace("dept. de ", "", ignoreCase = true)
    tv.text = replaceTwo
}

@BindingAdapter("requirementText")
fun requirementText(tv: TextView, requirement: FlowchartRequirementUI?) {
    requirement ?: return
    val ctx = tv.context
    val text = when {
        requirement.requiredDisciplineId != null -> WordUtils.toTitleCase(requirement.shownName)
        requirement.courseHours != null -> ctx.getString(R.string.flowchart_required_hours, requirement.courseHours)
        requirement.coursePercentage != null -> ctx.getString(R.string.flowchart_required_percentage, requirement.coursePercentage)
        else -> null
    }

    text ?: return
    tv.text = text
}