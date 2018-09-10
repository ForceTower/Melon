package com.forcetower.uefs.core.storage.database.accessors

import androidx.room.Embedded
import androidx.room.Relation
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassGroup

class ClassWithGroups {
    @Embedded
    lateinit var clazz: Class
    @Relation(parentColumn = "uid", entityColumn = "class_id", entity = ClassGroup::class)
    lateinit var groups: List<ClassGroupWithStudents>
}