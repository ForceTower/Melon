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

package com.forcetower.uefs.core.storage.repository

import android.content.SharedPreferences
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.forcetower.uefs.core.model.unes.EdgeParadoxSearchableItem
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.ParadoxService
import com.forcetower.uefs.domain.model.paradox.DisciplineCombinedData
import com.forcetower.uefs.domain.model.paradox.SemesterMean
import com.forcetower.uefs.domain.model.paradox.TeacherMean
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class EvaluationRepository @Inject constructor(
    private val database: UDatabase,
    private val service: ParadoxService,
    private val preferences: SharedPreferences
) {
    suspend fun getTrendingList() = service.hot().data

    suspend fun getDiscipline(id: String): DisciplineCombinedData {
        val data = service.discipline(id).data

        val teacherGrouped = data.teachers.groupBy { it.semesterPlatformId }
        val semesters = teacherGrouped.entries.map { entry ->
            val key = entry.key
            val semester = entry.value

            val studentCountWeighted = semester.sumOf { it.studentCountWeighted }
            val mean = if (studentCountWeighted > 0) {
                semester.sumOf { it.mean * it.studentCountWeighted } / studentCountWeighted
            } else {
                0.0
            }

            val first = semester.first()
            val startedAt = ZonedDateTime.parse(first.semesterStart, DateTimeFormatter.ISO_ZONED_DATE_TIME)
            SemesterMean(key, first.semester, mean, startedAt, studentCountWeighted)
        }.sortedBy { it.start }

        val teachers = data.teachers.groupBy { it.teacherId }.entries.map { entry ->
            val key = entry.key
            val values = entry.value.sortedBy {
                ZonedDateTime.parse(it.semesterStart, DateTimeFormatter.ISO_ZONED_DATE_TIME)
            }
            val first = values.first()
            val appear = values.maxBy {
                ZonedDateTime.parse(it.semesterStart, DateTimeFormatter.ISO_ZONED_DATE_TIME)
            }
            val studentCount = values.sumOf { it.studentsCount }

            val studentCountWeighted = values.sumOf { it.studentCountWeighted }
            val mean = if (studentCountWeighted > 0) {
                values.sumOf { it.mean * it.studentCountWeighted } / studentCountWeighted
            } else {
                0.0
            }

            val appearTime = ZonedDateTime.parse(appear.semesterStart, DateTimeFormatter.ISO_ZONED_DATE_TIME)

            TeacherMean(key, first.teacherName, appear.semester, mean, studentCount, studentCountWeighted, appearTime, values)
        }.sortedByDescending { it.semesterStart }

        return DisciplineCombinedData(
            data,
            semesters,
            teachers
        )
    }

    suspend fun getTeacherById(id: String) = service.teacher(id).data

    suspend fun downloadKnowledgeDatabase() {
        val update = preferences.getLong("_next_evaluation_knowledge_update_03_", 0)
        val calendar = Calendar.getInstance()
        if (update > calendar.timeInMillis) {
            return
        }

        try {
            val snapshot = service.all().data
            database.edgeParadoxSearchableItem.recreate(snapshot)
            calendar.add(Calendar.DAY_OF_YEAR, 2)
            preferences.edit().putLong("_next_evaluation_knowledge_update_03_", calendar.timeInMillis).apply()
        } catch (t: Throwable) {
            Timber.e("Failed to download knowledge database")
        }
    }

    fun queryEntities(queryProvider: () -> String): Flow<PagingData<EdgeParadoxSearchableItem>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                database.edgeParadoxSearchableItem.query(queryProvider())
            }
        ).flow
    }

//    fun getTeacherByName(teacherName: String): LiveData<Resource<EvaluationTeacher>> {
//        return object : NetworkOnlyResource<EvaluationTeacher>(executors) {
//            override fun createCall() = service.getTeacherByName(teacherName).asLiveData()
//            override fun saveCallResult(value: EvaluationTeacher) = Unit
//        }.asLiveData()
//    }
//
//    fun getQuestionsForTeacher(teacherId: Long): LiveData<Resource<List<Question>>> {
//        return object : NetworkOnlyResource<List<Question>>(executors) {
//            override fun createCall() = service.getQuestionsForTeachers(teacherId).asLiveData()
//            override fun saveCallResult(value: List<Question>) = Unit
//        }.asLiveData()
//    }
//
//    fun getQuestionsForDiscipline(code: String, department: String): LiveData<Resource<List<Question>>> {
//        return object : NetworkOnlyResource<List<Question>>(executors) {
//            override fun createCall() = service.getQuestionsForDisciplines(code, department).asLiveData()
//            override fun saveCallResult(value: List<Question>) = Unit
//        }.asLiveData()
//    }
//
//    @AnyThread
//    fun answer(data: MutableMap<String, Any?>) {
//        executors.networkIO().execute {
//            try {
//                val response = service.answerQuestion(data).execute()
//                if (response.isSuccessful) {
//                    Timber.d("Posted answer correctly")
//                } else {
//                    Timber.d("Answer failed with code ${response.code()}")
//                }
//            } catch (t: Throwable) {
//                Timber.e(t, "error on posting answer")
//            }
//        }
//    }
}
