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

package com.forcetower.uefs.feature.evaluation

import android.util.TypedValue
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.forcetower.core.utils.ViewUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.EvaluationEntity
import com.forcetower.uefs.feature.evaluation.discipline.SemesterMean
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

@BindingAdapter(value = ["evaluationGradesMean", "evaluationGradesTitle"], requireAll = true)
fun formatGradesMean(textView: TextView, list: List<SemesterMean>?, type: Int?) {
    list ?: return
    val context = textView.context
    val name = when (type) {
        1 -> context.getString(R.string.evaluation_teacher_single)
        else -> context.getString(R.string.evaluation_discipline_single)
    }
    val sum = list.map { it.mean.toFloat() }.sum()
    val mean = sum / list.size
    val text = context.getString(R.string.evaluation_general_mean, name, mean)
    textView.text = text
}

@BindingAdapter("semesterGradesForChart")
fun formatSemesterGradeChart(chart: LineChart, list: List<SemesterMean>?) {
    list ?: return
    val context = chart.context

    val pair = list.convertToDataSetWithTitles()
    val set = pair.second
    set.setDrawFilled(true)
    set.fillDrawable = ContextCompat.getDrawable(context, R.drawable.gradient_chart_evaluation)
    set.mode = LineDataSet.Mode.CUBIC_BEZIER
    set.color = ViewUtils.attributeColorUtils(context, R.attr.colorAccent)
    set.setDrawCircles(false)
    set.setDrawValues(false)

    val barData = LineData(set)
    val formatter = object : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            val converted = value.toInt()
            if (converted < 0 || converted >= pair.first.size)
                return ""
            return pair.first[converted]
        }
    }

    val typedValue = TypedValue()
    val theme = context.theme
    theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)
    val colorOnSurface = typedValue.data

    chart.apply {
        data = barData
        xAxis.apply {
            valueFormatter = formatter
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            description.isEnabled = false
            textColor = colorOnSurface
            setDrawGridLines(false)
            setDrawAxisLine(false)
        }
        axisLeft.apply {
            axisMaximum = 10f
            axisMinimum = 0f
            textColor = colorOnSurface
            setDrawGridLines(false)
            setDrawAxisLine(false)
            setDrawZeroLine(false)
        }
        axisRight.apply {
            isEnabled = false
        }
        legend.isEnabled = false
        setTouchEnabled(false)
        animateY(1000)
        invalidate()
    }
}

private fun List<SemesterMean>.convertToDataSetWithTitles(): Pair<List<String>, LineDataSet> {
    val sorted = this.sortedBy { it.id }
    val entries = sorted.mapIndexed { index, element -> Entry(index.toFloat(), element.mean.toFloat()) }
    val titles = sorted.map { it.name }
    return titles to LineDataSet(entries, "")
}

@BindingAdapter("evaluationEntityDescription")
fun evaluationEntityDescription(tv: TextView, entity: EvaluationEntity?) {
    entity ?: return
    val context = tv.context
    val string = when (entity.type) {
        0 -> context.getString(R.string.teacher_evaluation_entity)
        1 -> context.getString(R.string.discipline_evaluation_entity)
        2 -> context.getString(R.string.student_evaluation_entity, entity.extra)
        else -> context.getString(R.string.unknown_evaluation_entity)
    }
    tv.text = string
}
