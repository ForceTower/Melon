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