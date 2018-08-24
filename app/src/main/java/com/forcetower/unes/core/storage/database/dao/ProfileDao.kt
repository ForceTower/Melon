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

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.sagres.database.model.SPerson
import com.forcetower.sagres.utils.WordUtils
import com.forcetower.unes.core.model.Profile

@Dao
abstract class ProfileDao {
    @Insert(onConflict = REPLACE)
    abstract fun insert(profile: Profile)

    @Query("SELECT * FROM Profile WHERE me = 1 LIMIT 1")
    abstract fun selectMeDirect(): Profile?

    @Query("SELECT * FROM Profile WHERE me = 1 LIMIT 1")
    abstract fun selectMe(): LiveData<Profile>

    @Transaction
    open fun insert(person: SPerson, score: Double = -1.0) {
        val name = WordUtils.capitalize(person.name.trim())
        var profile = selectMeDirect()
        if (profile != null) {
            updateProfile(name, person.email.trim())
            if (score >= 0) updateScore(score)
        } else {
            profile = Profile(name = name, email = person.email.trim(), sagresId = person.id, me = true, score = score)
            insert(profile)
        }
    }

    @Query("UPDATE Profile SET score = :score WHERE me = 1")
    abstract fun updateScore(score: Double)

    @Query("UPDATE Profile SET name = :name, email = :email WHERE me = 1")
    abstract fun updateProfile(name: String, email: String)
}