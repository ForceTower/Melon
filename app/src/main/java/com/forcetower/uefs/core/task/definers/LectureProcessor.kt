package com.forcetower.uefs.core.task.definers

import android.content.Context
import androidx.room.withTransaction
import com.forcetower.uefs.core.model.unes.ClassItem
import com.forcetower.uefs.core.model.unes.ClassMaterial
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.task.UTask
import com.forcetower.uefs.service.NotificationCreator
import dev.forcetower.breaker.model.Lecture

class LectureProcessor(
    private val context: Context,
    private val database: UDatabase,
    // local database group id :)
    private val groupId: Long,
    private val lectures: List<Lecture>,
    private val notify: Boolean
) : UTask {
    override suspend fun execute() {
        database.withTransaction {
            lectures.forEach { lecture ->
                val current = database.classItemDao().getItemByIdentifiers(groupId, lecture.ordinal)
                val classId = if (current != null) {
                    current.copy(
                        date = lecture.date,
                        situation = lecture.situation.asLectureSituation(),
                        subject = lecture.subject,
                        numberOfMaterials = lecture.materials.size,
                        materialLinks = ""
                    )
                    database.classItemDao().update(current)
                    current.uid
                } else {
                    database.classItemDao().insert(
                        ClassItem(
                            number = lecture.ordinal,
                            groupId = groupId,
                            date = lecture.date,
                            isNew = true,
                            situation = lecture.situation.asLectureSituation(),
                            subject = lecture.subject,
                            numberOfMaterials = lecture.materials.size,
                            materialLinks = ""
                        )
                    )
                }

                lecture.materials.forEach { material ->
                    val mat = database.classMaterialDao().getMaterialsByIdentifiers(material.description, material.url, groupId)
                    if (mat != null) {
                        database.classMaterialDao().update(mat.copy(classItemId = classId))
                    } else {
                        database.classMaterialDao().insert(
                            ClassMaterial(
                                name = material.description,
                                isNew = true,
                                notified = !notify,
                                groupId = groupId,
                                link = material.url,
                                classItemId = classId
                            )
                        )
                    }
                }
            }

            val materials = database.classMaterialDao().getAllUnnotified()
            database.classMaterialDao().markAllNotified()
            materials.forEach {
                NotificationCreator.showMaterialPostedNotification(context, it)
            }
        }
    }

    private fun Int.asLectureSituation(): String {
        return when (this) {
            2 -> "realizada"
            else -> "pendente"
        }
    }
}