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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.forcetower.uefs.core.model.unes.Discipline
import timber.log.Timber

@Dao
abstract class DisciplineDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(discipline: Discipline): Long

    @Transaction
    open fun insert(disciplines: List<Discipline>) {
        disciplines.forEach { discipline ->
            Timber.d("Inserting discipline $discipline")
            val old = getDisciplineByCodeDirect(discipline.code)
            Timber.d("Old discipline is $old")
            if (old == null) {
                insert(discipline)
            } else {
                if (old.credits == 0 || discipline.credits > 0) {
                    updateDiscipline(old.uid, discipline.credits, discipline.name)
                }
            }
        }
    }

    @Query("UPDATE Discipline SET credits = :credits, name = :name WHERE uid = :uid")
    abstract fun updateDiscipline(uid: Long, credits: Int, name: String)

    @Query("SELECT * FROM Discipline WHERE LOWER(code) = LOWER(:code) LIMIT 1")
    abstract fun getDisciplineByCodeDirect(code: String): Discipline?

    @Transaction
    open suspend fun insertOrUpdate(value: Discipline): Long {
        val current = getDisciplineByCodeDirect(value.code)
        return if (current != null) {
            if (current != value) {
                update(value.copy(uid = current.uid))
            }
            current.uid
        } else {
            insert(value)
        }
    }

    @Update(onConflict = IGNORE)
    abstract suspend fun update(value: Discipline)
}
