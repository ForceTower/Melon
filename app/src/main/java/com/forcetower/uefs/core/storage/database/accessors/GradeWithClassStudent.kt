package com.forcetower.uefs.core.storage.database.accessors

import androidx.room.Embedded
import androidx.room.Relation
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassStudent
import com.forcetower.uefs.core.model.unes.Grade

class GradeWithClassStudent {
    @Embedded
    lateinit var grade: Grade
    @Relation(parentColumn = "class_id", entityColumn = "uid", entity = ClassStudent::class)
    lateinit var clazzes: List<ClassStudentWithGroup>

    fun clazz() = clazzes[0]
}