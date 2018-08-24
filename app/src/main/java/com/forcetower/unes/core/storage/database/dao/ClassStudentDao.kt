/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.unes.core.model.ClassGroup
import com.forcetower.unes.core.model.ClassStudent
import com.forcetower.unes.core.model.Profile

@Dao
abstract class ClassStudentDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(clazz: ClassStudent)

    @Transaction
    open fun joinGroups(groups: List<ClassGroup>) {
        val profile = getMeProfile()
        groups.forEach {
            insert(ClassStudent(groupId = it.uid, profileId = profile.uid))
        }
    }

    @Query("SELECT * FROM Profile WHERE me = 1")
    protected abstract fun getMeProfile(): Profile
}