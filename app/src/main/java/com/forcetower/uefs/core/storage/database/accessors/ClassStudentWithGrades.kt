package com.forcetower.uefs.core.storage.database.accessors

import androidx.room.Embedded
import androidx.room.Relation
import com.forcetower.uefs.core.model.unes.ClassStudent
import com.forcetower.uefs.core.model.unes.Grade

class ClassStudentWithGrades {
    @Embedded
    lateinit var student: ClassStudent
    @Relation(parentColumn = "uid", entityColumn = "class_id", entity = Grade::class)
    lateinit var grades: List<Grade>
}
