package com.forcetower.uefs.feature.disciplines

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.core.storage.database.accessors.ClassGroupWithStudents
import com.forcetower.uefs.feature.grades.ClassGroupGradesAdapter

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
        list.addAll(it.students[0].grades)
    }
    return list
}
