package com.forcetower.uefs.core.task.definers

import android.content.Context
import androidx.room.withTransaction
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.task.UTask
import com.forcetower.uefs.service.NotificationCreator
import dev.forcetower.breaker.model.LectureMissed

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
        database.withTransaction {
            val group = database.classGroupDao().getGroupDirect(groupId) ?: return@withTransaction
            val classId = group.classId
            var current = database.classAbsenceDao().getAbsenceFromClassDirect(classId)
            database.classAbsenceDao().resetClassAbsences(classId)


            // N * (2*N)
            absences.forEach { absence ->
                val found = current.find {
                    it.classId == classId && it.profileId == profileId && it.sequence == absence.lecture.ordinal && it.grouping == group.group
                }

                if (found != null) {
                    current = current.filter { it.uid == found.uid }
                    val updated = found.copy(
                        description = absence.lecture.subject,
                        date = absence.lecture.date
                    )
                    database.classAbsenceDao().update(updated)
                } else {
                    database.classAbsenceDao().insert(
                        ClassAbsence(
                            description = absence.lecture.subject,
                            date = absence.lecture.date,
                            notified = !notify,
                            profileId = profileId,
                            grouping = group.group,
                            classId = classId,
                            sequence = absence.lecture.ordinal
                        )
                    )
                }
            }

            if (notify) {
                val newAbsences = database.classAbsenceDao().getUnnotifiedDirect()
                newAbsences.forEach {
                    NotificationCreator.showAbsenceNotification(it, context, true)
                }
                current.forEach {
                    NotificationCreator.showAbsenceNotification(it, context, false)
                }
            }

            database.classAbsenceDao().markAllNotified()
        }
    }
}