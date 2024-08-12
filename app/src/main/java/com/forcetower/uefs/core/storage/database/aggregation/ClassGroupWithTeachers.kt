package com.forcetower.uefs.core.storage.database.aggregation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.forcetower.uefs.core.model.unes.AffinityQuestionAlternative
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.Teacher

data class ClassGroupWithTeachers(
    @Embedded
    val data: ClassGroup,
    @Relation(
        entity = Teacher::class,
        entityColumn = "id",
        parentColumn = "id",
        associateBy = Junction(value = AffinityQuestionAlternative::class, parentColumn = "classGroupId", entityColumn = "teacherId")
    )
    val teachers: List<Teacher>
)