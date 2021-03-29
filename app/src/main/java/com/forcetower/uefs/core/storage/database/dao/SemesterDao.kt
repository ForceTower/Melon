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
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.uefs.core.model.unes.Semester

@Dao
abstract class SemesterDao {
    @Transaction
    open fun insertIgnoring(semesters: List<Semester>) {
        val newSemesters = semesters.filter { semester ->
            val current = getSemesterDirect(semester.sagresId)
            if (current != null) {
                if (current.start != semester.start && semester.start != null)
                    updateStart(current.sagresId, semester.start)

                if (current.end != semester.end && semester.end != null)
                    updateEnd(current.sagresId, semester.end)

                if (current.startClass != semester.startClass && semester.startClass != null)
                    updateStartClass(current.sagresId, semester.startClass)

                if (current.endClass != semester.endClass && semester.endClass != null)
                    updateEndClass(current.sagresId, semester.endClass)

                if (current.name != semester.name)
                    updateName(current.sagresId, semester.name)
            }

            // keep only the new semesters
            // don't bother try inserting if we just updated above
            current == null
        }
        internalInsertIgnoring(newSemesters)
    }

    @Query("UPDATE Semester SET start = :start WHERE sagres_id = :sagresId")
    protected abstract fun updateStart(sagresId: Long, start: Long)

    @Query("UPDATE Semester SET `end` = :end WHERE sagres_id = :sagresId")
    protected abstract fun updateEnd(sagresId: Long, end: Long)

    @Query("UPDATE Semester SET start_class = :startClass WHERE sagres_id = :sagresId")
    protected abstract fun updateStartClass(sagresId: Long, startClass: Long)

    @Query("UPDATE Semester SET end_class = :endClass WHERE sagres_id = :sagresId")
    protected abstract fun updateEndClass(sagresId: Long, endClass: Long)

    @Query("UPDATE Semester SET name = :name WHERE sagres_id = :sagresId")
    protected abstract fun updateName(sagresId: Long, name: String)

    @Query("SELECT * FROM Semester WHERE sagres_id = :sagresId")
    abstract fun getSemesterDirect(sagresId: Long): Semester?

    @Insert(onConflict = IGNORE)
    protected abstract fun internalInsertIgnoring(semesters: List<Semester>)

    @Insert(onConflict = IGNORE)
    abstract fun insertIgnoring(semester: Semester)

    @Query("SELECT * FROM Semester ORDER BY sagres_id DESC")
    abstract fun getParticipatingSemesters(): LiveData<List<Semester>>

    @Query("SELECT * FROM Semester ORDER BY sagres_id DESC")
    abstract suspend fun getParticipatingSemestersDirect(): List<Semester>

    @Query("DELETE FROM Semester")
    abstract fun deleteAll()

    @Query("SELECT * FROM Semester ORDER BY sagres_id DESC")
    abstract fun getSemestersDirect(): List<Semester>
}
