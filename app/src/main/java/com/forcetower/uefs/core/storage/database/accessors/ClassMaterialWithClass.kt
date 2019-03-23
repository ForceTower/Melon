package com.forcetower.uefs.core.storage.database.accessors

import androidx.room.Embedded
import androidx.room.Relation
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.ClassMaterial

class ClassMaterialWithClass {
    @Embedded
    lateinit var material: ClassMaterial
    @Relation(parentColumn = "group_id", entityColumn = "uid", entity = ClassGroup::class)
    lateinit var groups: List<GroupWithClass>

    fun group() = groups.firstOrNull()
}