/*
 * Copyright (c) 2018.
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

package com.forcetower.uefs.feature.disciplines

import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.core.storage.database.accessors.ClassGroupWithStudents
import com.forcetower.uefs.core.storage.database.accessors.ClassWithGroups
import com.forcetower.uefs.feature.grades.ClassGroupGradesAdapter
import com.forcetower.uefs.widget.CircleProgressBar

@BindingAdapter("disciplineGroupsGrades")
fun disciplineGroupsGrades(recycler: RecyclerView, classes: List<ClassGroupWithStudents>) {
    val list = generateGradesList(classes)
    recycler.adapter = (recycler.adapter as? ClassGroupGradesAdapter?: ClassGroupGradesAdapter()).apply {
        submitList(list)
    }
}

fun generateGradesList(classes: List<ClassGroupWithStudents>): List<Grade>? {
    val list = ArrayList<Grade>()
    classes.forEach {
        if (it.students.isNotEmpty())
            list.addAll(it.students[0].grades)
    }
    list.sortBy { it -> it.name }
    return list
}

@BindingAdapter("classStudentGrade")
fun classStudentGrade(cpb: CircleProgressBar, clazz: ClassWithGroups) {
    val value: Double? = getClassWithGroupsGrade(clazz)
    if (value == null) {
        cpb.setProgress(0.0f)
    } else {
        cpb.setProgressWithAnimation(value.toFloat() * 10)
    }
}

@BindingAdapter("classStudentGrade")
fun classStudentGrade(tv: TextView, clazz: ClassWithGroups) {
    val value = getClassWithGroupsGrade(clazz)
    if (value == null) {
        tv.text = "??"
    } else {
        tv.text = value.toString()
    }
}

fun getClassWithGroupsGrade(clazz: ClassWithGroups): Double? {
    if (clazz.groups.isNotEmpty()) {
        val students = clazz.groups[0].students
        if (students.isNotEmpty())
            return students[0].student.finalScore
    }
    return null
}

@BindingAdapter(value = ["missedDescription", "missedDate"], requireAll = true)
fun classAbsence(tv: TextView, desc: String, date: String) {
    tv.text = tv.context.getString(R.string.discipline_absence_item_format, desc, date)
}

@BindingAdapter(value = ["absences", "credits"], requireAll = true)
fun totalAbscence(tv: TextView, absences: Int, credits: Int?) {
    val context = tv.context
    if (credits == null || credits == 0) {
        tv.text = context.getString(R.string.discipline_credits_undefined)
    } else {
        val left = (credits/4) - absences
        tv.text = context.getString(R.string.discipline_absence_left, left)
    }
}