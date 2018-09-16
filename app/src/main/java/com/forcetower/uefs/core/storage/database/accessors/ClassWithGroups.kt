package com.forcetower.uefs.core.storage.database.accessors

import androidx.room.Embedded
import androidx.room.Relation
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.Discipline

class ClassWithGroups {
    @Embedded
    lateinit var clazz: Class
    @Relation(parentColumn = "uid", entityColumn = "class_id", entity = ClassGroup::class)
    lateinit var groups: List<ClassGroupWithStudents>
    @Relation(parentColumn = "discipline_id", entityColumn = "uid", entity = Discipline::class)
    lateinit var disciplines: List<Discipline>

    fun discipline() = disciplines[0]
}