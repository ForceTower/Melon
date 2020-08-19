package com.forcetower.uefs.core.task.definers

import androidx.room.withTransaction
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.task.UTask

class SemestersProcessor(
    private val database: UDatabase,
    private val semesters: List<dev.forcetower.breaker.model.Semester>
) : UTask {
    override suspend fun execute() {
        database.withTransaction {
            semesters.forEach {
                val semester = Semester(sagresId = it.id, name = it.code, codename = it.description)
                database.semesterDao().insertIgnoring(semester)
            }
        }
    }
}