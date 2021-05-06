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

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.uefs.core.model.api.EverythingSnippet
import com.forcetower.uefs.core.model.unes.EvaluationEntity
import com.forcetower.uefs.core.util.unaccent
import java.util.Locale

@Dao
abstract class EvaluationEntitiesDao {
    @Transaction
    open fun recreate(data: EverythingSnippet) {
        clear()
        val teachers = data.teachers.map { EvaluationEntity(0, it.teacherId, it.name, null, it.imageUrl, 0, it.name.toLowerCase(Locale.getDefault()).unaccent()) }
        val disciplines = data.disciplines.map {
            val coded = "${it.department}${it.code}"
            val search = "$coded ${it.name}".toLowerCase(Locale.getDefault()).unaccent()
            EvaluationEntity(0, it.disciplineId, it.name, coded, null, 1, search, comp1 = it.department, comp2 = it.code)
        }
        val students = data.students.map {
            val search = "${it.name} ${it.courseName}".toLowerCase(Locale.getDefault()).unaccent()
            EvaluationEntity(0, it.id, it.name, it.courseName, it.imageUrl, 2, search, referenceLong1 = it.userId)
        }

        val batch = mutableListOf<EvaluationEntity>()
        batch.apply {
            addAll(teachers)
            addAll(disciplines)
            addAll(students)
        }
        insert(batch)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insert(values: List<EvaluationEntity>)

    @Query("DELETE FROM EvaluationEntity")
    protected abstract fun clear()

    open fun query(query: String): PagingSource<Int, EvaluationEntity> {
        return if (query.isBlank()) {
            doQueryEmpty()
        } else {
            val realQuery = query.toLowerCase(Locale.getDefault())
                .unaccent()
                .split(" ")
                .joinToString("%", "%", "%")
            doQuery(realQuery)
        }
    }

    @Query("SELECT * FROM EvaluationEntity WHERE LOWER(searchable) LIKE :query ORDER BY name")
    abstract fun doQuery(query: String): PagingSource<Int, EvaluationEntity>

    @Query("SELECT * FROM EvaluationEntity WHERE 0")
    abstract fun doQueryEmpty(): PagingSource<Int, EvaluationEntity>
}
