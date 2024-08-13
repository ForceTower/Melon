package com.forcetower.uefs.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.forcetower.core.database.BaseDao
import com.forcetower.uefs.core.model.unes.ClassGroupTeacher

@Dao
abstract class ClassGroupTeacherDao : BaseDao<ClassGroupTeacher>() {
    @Query("DELETE FROM ClassGroupTeacher WHERE classGroupId = :classGroupId")
    abstract suspend fun deleteAllFromClassGroup(classGroupId: Long)
}