package com.forcetower.uefs.core.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.UDatabase
import javax.inject.Inject

class DisciplineViewModel @Inject constructor(
    private val database: UDatabase
): ViewModel() {
    val semesters: LiveData<List<Semester>> by lazy { database.semesterDao().getParticipatingSemesters() }
}