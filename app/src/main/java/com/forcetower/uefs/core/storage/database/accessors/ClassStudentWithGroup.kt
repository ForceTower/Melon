package com.forcetower.uefs.core.storage.database.accessors

import androidx.room.Embedded
import androidx.room.Relation
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.ClassStudent

class ClassStudentWithGroup {
    @Embedded
    lateinit var classStudent: ClassStudent
    @Relation(parentColumn = "group_id", entityColumn = "uid", entity = ClassGroup::class)
    lateinit var groups: List<GroupWithClass>

    fun group() = groups[0]
}
