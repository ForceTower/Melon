/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package dev.forcetower.disciplines.feature

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup
import com.forcetower.uefs.core.util.round
import com.forcetower.uefs.feature.shared.extensions.makeSemester
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@BindingAdapter("semesterName")
fun TextView.semesterName(semester: Semester?) {
    semester ?: return
    text = semester.codename.makeSemester()
}

@BindingAdapter("evaluationDate")
fun evaluationDate(tv: TextView, value: Grade?) {
    val date = value?.date
    if (date == null) {
        tv.text = tv.context.getString(R.string.grade_date_unknown)
    } else {
        try {
            tv.text = OffsetDateTime.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
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

@BindingAdapter("gradeFormat")
fun gradeFormat(tv: TextView, value: Grade?) {
    val grade = value?.gradeDouble()
    if (grade == null) {
        tv.text = tv.context.getString(R.string.grade_not_published)
    } else {
        tv.text = tv.context.getString(R.string.grade_format, grade.toFloat())
    }
}
