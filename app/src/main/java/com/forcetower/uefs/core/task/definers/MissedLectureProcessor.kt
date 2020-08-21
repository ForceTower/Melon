package com.forcetower.uefs.core.task.definers

import android.content.Context
import androidx.room.withTransaction
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.task.UTask
import com.forcetower.uefs.service.NotificationCreator
import dev.forcetower.breaker.model.LectureMissed
import timber.log.Timber

class MissedLectureProcessor(
    private val context: Context,
    private val database: UDatabase,
    // local database profile id
    private val profileId: Long,
    // local database group id :)
    private val groupId: Long,
    private val absences: List<LectureMissed>,
    private val notify: Boolean
) : UTask {
    override suspend fun execute() {
        Timber.d("Why is this executing?")
        database.withTransaction {
            val group = database.classGroupDao().getGroupDirect(groupId) ?: return@withTransaction
            val classId = group.classId
            database.classAbsenceDao().resetClassAbsences(classId)

            val mapped = absences.map { absence ->
                ClassAbsence(
                    description = absence.lecture.subject,
                    date = absence.lecture.date,
                    notified = !notify,
                    profileId = profileId,
                    grouping = group.group,
                    classId = classId,
                    sequence = absence.lecture.ordinal
                )
            }

            database.classAbsenceDao().insert(mapped)
            database.classAbsenceDao().markAllNotified()
        }
    }
}