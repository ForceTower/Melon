package com.forcetower.uefs.core.storage.database.accessors

import androidx.room.Embedded
import androidx.room.Relation
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.ClassStudent

class ClassGroupWithStudents {
    @Embedded
    lateinit var group: ClassGroup
    @Relation(parentColumn = "uid", entityColumn = "group_id", entity = ClassStudent::class)
    lateinit var students: List<ClassStudentWithGrades>
}