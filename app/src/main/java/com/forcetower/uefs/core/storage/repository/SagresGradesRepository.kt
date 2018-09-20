package com.forcetower.uefs.core.storage.repository

import androidx.annotation.WorkerThread
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.SDisciplineMissedClass
import com.forcetower.sagres.database.model.SGrade
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.UDatabase
import timber.log.Timber
import javax.inject.Inject

class SagresGradesRepository @Inject constructor(
    val database: UDatabase
) {
    @WorkerThread
    fun getGrades(semesterSagresId: Long, needLogin: Boolean = true): Int {
        val access = database.accessDao().getAccessDirect()
        access?: return NO_ACCESS

        return if (needLogin) {
            val login = SagresNavigator.instance.login(access.username, access.password)
            if (login.status == Status.SUCCESS && login.document != null) {
                Timber.d("[$semesterSagresId] Login Completed Correctly")
                proceed(semesterSagresId)
            } else {
                INVALID_ACCESS
            }
        } else {
            proceed(semesterSagresId)
        }
    }

    @WorkerThread
    private fun proceed(semesterSagresId: Long): Int {
        val grades = SagresNavigator.instance.getCurrentGrades()
        return if (grades.status == Status.SUCCESS && grades.document != null) {
            Timber.d("[$semesterSagresId] Grades Part 01/02 Completed!")
            val semesterGrades = SagresNavigator.instance.getGradesFromSemester(semesterSagresId, grades.document!!)
            if (semesterGrades.status == Status.SUCCESS) {
                defineSemesters(semesterGrades.semesters)
                defineGrades(semesterGrades.grades)
                defineFrequency(semesterGrades.frequency)
                Timber.d("[$semesterSagresId] Grades Part 02/02 Completed!")
                Timber.d("[$semesterSagresId] Grades: ${semesterGrades.grades}")
                SUCCESS
            } else {
                ACTUAL_GRADES_CALL_FAILED
            }
        } else {
            Timber.d("Current Grades Status Failed")
            CURRENT_GRADES_FAILED
        }
    }

    @WorkerThread
    private fun defineFrequency(frequency: List<SDisciplineMissedClass>?) {
        if (frequency == null) return
        database.classAbsenceDao().putAbsences(frequency)
    }

    @WorkerThread
    private fun defineGrades(grades: List<SGrade>) {
        database.gradesDao().putGrades(grades)
    }

    @WorkerThread
    private fun defineSemesters(semesters: List<Pair<Long, String>>) {
        semesters.forEach {
            val semester = Semester(sagresId = it.first, name = it.second, codename = it.second)
            database.semesterDao().insertIgnoring(semester)
        }
    }

    companion object {
        const val SUCCESS = 0
        const val NO_ACCESS = -1
        const val INVALID_ACCESS = -2
        const val CURRENT_GRADES_FAILED = -3
        const val ACTUAL_GRADES_CALL_FAILED = -4
    }
}