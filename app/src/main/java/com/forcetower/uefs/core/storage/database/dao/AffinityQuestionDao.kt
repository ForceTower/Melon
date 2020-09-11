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

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.uefs.core.model.service.AffinityQuestionDTO
import com.forcetower.uefs.core.model.unes.AffinityQuestion
import com.forcetower.uefs.core.model.unes.AffinityQuestionAlternative
import com.forcetower.uefs.core.model.unes.SStudent
import com.forcetower.uefs.core.storage.database.aggregation.AffinityQuestionFull

@Dao
abstract class AffinityQuestionDao {
    @Transaction
    @Query("SELECT * FROM AffinityQuestion WHERE answered = 0")
    abstract fun getUnansweredQuestions(): LiveData<List<AffinityQuestionFull>>

    @WorkerThread
    @Transaction
    open fun insert(values: List<AffinityQuestionDTO>) {
        values.forEach { value ->
            val exists = getQuestionDirect(value.id)
            if (exists == null) {
                val question = AffinityQuestion(value.id, value.question, answered = false, synced = false)
                val alternatives = value.alternatives.map { AffinityQuestionAlternative(questionId = question.id, studentId = it.id) }
                insertQuestion(question)
                insertStudents(value.alternatives)
                // Must follow this order because of constraints :)
                insertAlternatives(alternatives)
            }
        }
    }

    @WorkerThread
    @Insert(onConflict = IGNORE)
    protected abstract fun insertQuestion(value: AffinityQuestion)

    @WorkerThread
    @Query("SELECT * FROM AffinityQuestion WHERE id = :id")
    protected abstract fun getQuestionDirect(id: Long): AffinityQuestion?

    @WorkerThread
    @Insert(onConflict = REPLACE)
    protected abstract fun insertStudents(values: List<SStudent>)

    @WorkerThread
    @Insert(onConflict = REPLACE)
    protected abstract fun insertAlternatives(values: List<AffinityQuestionAlternative>)

    @WorkerThread
    @Query("UPDATE AffinityQuestion SET answered = 1 WHERE id = :questionId")
    abstract fun answerQuestion(questionId: Long)

    @WorkerThread
    @Query("UPDATE AffinityQuestion SET synced = 1 WHERE id = :questionId")
    abstract fun markSynced(questionId: Long)
}
