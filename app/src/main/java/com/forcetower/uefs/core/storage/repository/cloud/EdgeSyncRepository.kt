package com.forcetower.uefs.core.storage.repository.cloud

import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.EdgeService
import com.forcetower.uefs.feature.shared.extensions.toTitleCase
import dagger.Reusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@Reusable
class EdgeSyncRepository @Inject constructor(
    private val service: EdgeService,
    private val database: UDatabase
) {
    suspend fun syncDataIfNeeded() {
        database.edgeAccessToken.require() ?: return
        Timber.d("Has edge token!!")

        messages()
        disciplines()
    }

    private suspend fun messages() {
        val messages = service.messages().data
        val converted = messages.map {
            Message.fromMessage(it, true)
        }
        withContext(Dispatchers.IO) {
            database.messageDao().insertIgnoring(converted)
        }
    }

    private suspend fun disciplines() {
        val (semester, disciplines) = service.disciplines().data
        database.semesterDao().insertIgnoreSuspend(
            Semester(
                sagresId = semester.platformId,
                name = semester.name,
                codename = semester.codename,
                start = semester.start?.toInstant()?.toEpochMilli(),
                end = semester.finish?.toInstant()?.toEpochMilli(),
                startClass = semester.start?.toInstant()?.toEpochMilli(),
                endClass = semester.finish?.toInstant()?.toEpochMilli()
            )
        )

        val internalSemesterId = database.semesterDao().getSemesterDirectSuspend(semester.platformId)?.uid ?: return
        withContext(Dispatchers.IO) {
            disciplines.forEach { disciplineData ->
                val discipline = disciplineData.discipline
                val internalDisciplineId = database.disciplineDao().insertOrUpdate(
                    Discipline(
                        name = discipline.name,
                        code = discipline.code,
                        department = discipline.department.toTitleCase(),
                        credits = discipline.credits,
                        resume = discipline.program
                    )
                )

                database.classDao().insert(
                    Class(
                        disciplineId = internalDisciplineId,
                        semesterId = internalSemesterId
                    )
                )

                val internalClass = database.classDao().getClassDirectlyNew(internalSemesterId, internalDisciplineId) ?: return@forEach

                database.classGroupDao().insert(
                    ClassGroup(
                        classId = internalClass.uid,
                        group = disciplineData.sequence,
                        credits = disciplineData.creditsOverride
                    )
                )

                val grades = disciplineData.grades
                database.gradesDao().putEdgeGrades(grades, internalClass.uid)
            }
        }
    }
}