package com.forcetower.unes.core.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.forcetower.unes.core.model.unes.Semester
import com.forcetower.unes.core.storage.database.UDatabase
import javax.inject.Inject

class DisciplineViewModel @Inject constructor(
    private val database: UDatabase
): ViewModel() {
    val semesters: LiveData<List<Semester>> by lazy { database.semesterDao().getParticipatingSemesters() }
}