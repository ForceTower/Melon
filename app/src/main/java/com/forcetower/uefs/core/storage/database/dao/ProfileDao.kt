/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.sagres.database.model.SagresPerson
import com.forcetower.sagres.utils.WordUtils
import com.forcetower.uefs.core.model.unes.Profile
import dev.forcetower.breaker.model.Person
import timber.log.Timber
import java.util.Locale

@Dao
abstract class ProfileDao {
    @Insert(onConflict = REPLACE)
    abstract fun insert(profile: Profile): Long

    @Query("SELECT * FROM Profile WHERE me = 1 LIMIT 1")
    abstract fun selectMeDirect(): Profile?

    @Query("SELECT * FROM Profile WHERE me = 1 LIMIT 1")
    abstract fun selectMe(): LiveData<Profile?>

    @Transaction
    open fun insert(person: SagresPerson, score: Double = -1.0) {
        val name = WordUtils.toTitleCase(person.name?.trim()) ?: ""
        var profile = selectMeDirect()
        if (profile != null) {
            updateProfileName(name)
            updateProfileMockStatus(person.isMocked)
            if (!person.isMocked) {
                updateProfileEmail(person.email?.trim() ?: "")
                Timber.d("Updating profile sagres id to ${person.id} ${person.sagresId}")
                updateProfileSagresId(person.id)
            }
            if (score >= 0) updateScore(score)
        } else {
            profile = Profile(name = name, email = person.email?.trim(), sagresId = person.id, me = true, score = score)
            insert(profile)
        }
    }

    @Transaction
    open suspend fun insert(person: Person): Long {
        val name = WordUtils.toTitleCase(person.name) ?: ""
        val me = selectMeDirect()
        if (me != null) {
            updateProfileName(name)
            updateProfileMockStatus(false)
            updateProfileSagresId(person.id)
            person.email?.toLowerCase(Locale.getDefault())?.let {
                updateProfileEmail(it)
            }
            return me.uid
        } else {
            return insert(
                Profile(
                    name = name,
                    email = person.email,
                    sagresId = person.id,
                    me = true
                )
            )
        }
    }

    @Query("SELECT c.name FROM Profile p, Course c WHERE p.course IS NOT NULL AND p.course = c.id LIMIT 1")
    abstract fun getProfileCourse(): LiveData<String?>

    @Query("SELECT c.name FROM Profile p, Course c WHERE p.course IS NOT NULL AND p.course = c.id LIMIT 1")
    abstract fun getProfileCourseDirect(): String?

    @Query("UPDATE Profile SET score = :score")
    abstract fun updateScore(score: Double)

    @Query("UPDATE Profile SET mocked = :mocked")
    abstract fun updateProfileMockStatus(mocked: Boolean)

    @Query("UPDATE Profile SET name = :name")
    abstract fun updateProfileName(name: String)

    @Query("UPDATE Profile SET email = :email")
    abstract fun updateProfileEmail(email: String)

    @Query("UPDATE Profile SET sagres_id = :sagresId")
    abstract fun updateProfileSagresId(sagresId: Long)

    @Query("DELETE FROM Profile WHERE me = 1")
    abstract fun deleteMe()

    @Query("SELECT * FROM Profile WHERE uuid = :profileUUID LIMIT 1")
    abstract fun selectProfileByUUID(profileUUID: String): LiveData<Profile?>

    @Query("UPDATE Profile SET course = :courseId WHERE me = 1")
    abstract fun updateCourse(courseId: Long)

    @Query("UPDATE Profile SET calc_score = :score WHERE me = 1")
    abstract fun updateCalculatedScore(score: Double)
}
