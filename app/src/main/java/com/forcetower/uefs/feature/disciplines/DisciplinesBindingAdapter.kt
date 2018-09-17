package com.forcetower.uefs.feature.disciplines

import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
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
    return list
}

@BindingAdapter("classStudentGrade")
fun classStudentGrade(cpb: CircleProgressBar, clazz: ClassWithGroups) {
    val value: Double? = getClassWithGroupsGrade(clazz)

    if (value == null) {
        cpb.setProgress(0.0f)
    } else {
        cpb.setProgressWithAnimation(value.toFloat() * 100)
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