package com.forcetower.sagres.operation.semester

import com.forcetower.sagres.database.model.Semester
import com.forcetower.sagres.operation.BaseCallback
import com.forcetower.sagres.operation.Status

class SemesterCallback(status: Status) : BaseCallback<SemesterCallback>(status) {
    private var semesters: List<Semester> = ArrayList()

    fun semesters(semesters: List<Semester>): SemesterCallback {
        this.semesters = semesters
        return this
    }

    fun getSemesters(): List<Semester> = semesters
}