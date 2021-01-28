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

package com.forcetower.uefs.feature.disciplines

import android.graphics.Paint
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup
import com.forcetower.uefs.core.util.round
import com.forcetower.uefs.feature.common.DisciplineActions
import com.forcetower.uefs.feature.grades.ClassGroupGradesAdapter
import com.forcetower.uefs.widget.CircleProgressBar
import timber.log.Timber
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

@BindingAdapter(value = ["disciplineGroupsGrades", "disciplineListener"], requireAll = false)
fun disciplineGroupsGrades(recycler: RecyclerView, classes: List<Grade>?, listener: DisciplineActions?) {
    val sort = classes?.sortedWith { one, two ->
        when {
            one.name.trim().equals("prova final", ignoreCase = true) -> 1
            two.name.trim().equals("prova final", ignoreCase = true) -> -1
            else -> one.name.compareTo(two.name)
        }
    }

    val adapter: ClassGroupGradesAdapter
    if (recycler.adapter == null) {
        adapter = ClassGroupGradesAdapter(listener)
        recycler.adapter = adapter
    } else {
        adapter = recycler.adapter as ClassGroupGradesAdapter
    }

    adapter.submitList(sort)
}

@BindingAdapter("classStudentGrade")
fun classStudentGrade(cpb: CircleProgressBar, clazz: ClassFullWithGroup?) {
    val value = clazz?.clazz?.finalScore
    if (value == null) {
        cpb.setProgress(0.0f)
    } else {
        cpb.setProgressWithAnimation(value.toFloat() * 10)
    }
}

@BindingAdapter("gradeFormat")
fun gradeFormat(tv: TextView, value: Grade?) {
    val grade = value?.gradeDouble()
    if (grade == null) {
        tv.text = tv.context.getString(R.string.grade_not_published)
    } else {
        tv.text = tv.context.getString(R.string.grade_format, grade.toFloat())
    }
}

@BindingAdapter("evaluationDate")
fun evaluationDate(tv: TextView, value: Grade?) {
    val date = value?.date
    if (date == null) {
        tv.text = tv.context.getString(R.string.grade_date_unknown)
    } else {
        try {
            tv.text = OffsetDateTime.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/YYYY"))
        } catch (error: Throwable) {
            tv.text = date
        }
    }
}

@BindingAdapter("classStudentGrade")
fun classStudentGrade(tv: TextView, clazz: ClassFullWithGroup?) {
    val value = clazz?.clazz?.finalScore
    if (value == null) {
        tv.text = "??"
    } else {
        tv.text = value.toString()
    }
}

@BindingAdapter("gradeNeededInFinal")
fun gradeNeededInFinal(tv: TextView, clazz: ClassFullWithGroup?) {
    val value = clazz?.clazz?.partialScore
    if (value == null) {
        tv.text = "??"
    } else {
        val needed = (12.5 - (1.5 * value)).round()
        tv.text = tv.context.getString(R.string.grade_format, needed)
    }
}

fun getClassWithGroupsGrade(clazz: ClassFullWithGroup): Double? {
    if (clazz.groups.isNotEmpty()) {
        return clazz.clazz.finalScore
    }
    return null
}

@BindingAdapter(value = ["missedDescription", "missedDate"], requireAll = true)
fun classAbsence(tv: TextView, desc: String?, date: String?) {
    tv.text = tv.context.getString(R.string.discipline_absence_item_format, desc ?: tv.context.getString(R.string.not_registed), date ?: tv.context.getString(R.string.not_registed))
}

@BindingAdapter(value = ["absences", "absencesListSize", "credits"], requireAll = true)
fun totalAbsence(tv: TextView, absences: Int?, absencesListSize: Int?, credits: Int?) {
    val context = tv.context
    val absenceCount = max(absences ?: 0, absencesListSize ?: 0)
    if ((absences == null && absencesListSize == null) || credits == null || credits == 0) {
        tv.text = context.getString(R.string.discipline_credits_undefined)
    } else {
        Timber.d("Credits: $credits __ Absence: $absences")
        val left = (credits / 4) - absenceCount
        when {
            left > 0 -> tv.text = context.getString(R.string.discipline_absence_left, left)
            left == 0 -> tv.text = context.getString(R.string.you_cant_miss_a_class)
            else -> tv.text = context.getString(R.string.you_missed_to_many_classes)
        }
    }
}

@BindingAdapter(value = ["absenceCount", "absenceAmount"], requireAll = true)
fun absenceCount(tv: TextView, absenceCount: Int?, absenceAmount: Int?) {
    val context = tv.context
    val count = max(absenceCount ?: 0, absenceAmount ?: 0)
    if (absenceCount == null && absenceAmount == null) {
        tv.text = context.getString(R.string.discipline_credits_undefined)
    } else {
        tv.text = context.getString(R.string.integer_format, count)
    }
}

@BindingAdapter(value = ["disciplineCredits"])
fun credits(tv: TextView, credits: Int?) {
    tv.text = credits?.toString()?.plus("h") ?: "??h"
}

@BindingAdapter(value = ["somethingOrQuestions"])
fun somethingOrQuestions(tv: TextView, something: String?) {
    val text = something ?: "????"
    tv.text = text
}

@BindingAdapter(value = ["classSubject", "classSituation"], requireAll = true)
fun classSubject(tv: TextView, subject: String?, situation: String?) {
    val text = subject ?: "????"
    tv.text = text

    val strike = situation?.trim()?.equals("realizada", ignoreCase = true)

    if (strike == true) tv.paintFlags = tv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    else tv.paintFlags = tv.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
}

@BindingAdapter(value = ["absenceSequence", "absenceDate"], requireAll = true)
fun disciplineAbsence(tv: TextView, sequence: Int?, date: String?) {
    val ctx = tv.context
    val seq = sequence ?: 0
    val dat = date ?: "??/??/????"

    val dated = try {
        OffsetDateTime.parse(dat).format(DateTimeFormatter.ofPattern("dd/MM/YYYY"))
    } catch (error: Throwable) {
        dat
    }

    val text = ctx.getString(R.string.discipline_absence_date_format, seq, dated)
    tv.text = text
}

@BindingAdapter(value = ["absenceDescription"])
fun absenceDescription(tv: TextView, description: String?) {
    val desc = description ?: "CL 2 - ????"
    val text = desc.substring(desc.indexOf("-") + 1).trim()
    tv.text = text
}

@BindingAdapter(value = ["disciplineStartsAtText", "disciplineEndsAtText"])
fun disciplineStartEndGenerator(tv: TextView, startsAt: String?, endsAt: String?) {
    val context = tv.context
    tv.text = context.getString(R.string.discipline_start_end_format, startsAt, endsAt)
}
