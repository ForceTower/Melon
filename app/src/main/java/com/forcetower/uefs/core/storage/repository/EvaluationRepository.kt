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
import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.service.EvaluationDiscipline
import com.forcetower.uefs.core.model.service.EvaluationHomeTopic
import com.forcetower.uefs.core.model.service.EvaluationTeacher
import com.forcetower.uefs.core.model.unes.EvaluationEntity
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.network.adapter.asLiveData
import com.forcetower.uefs.core.storage.resource.NetworkOnlyResource
import com.forcetower.uefs.core.storage.resource.Resource
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

class EvaluationRepository @Inject constructor(
    private val database: UDatabase,
    private val service: UService,
    private val executors: AppExecutors,
    private val preferences: SharedPreferences
) {
    fun getTrendingList(): LiveData<Resource<List<EvaluationHomeTopic>>> {
        return object : NetworkOnlyResource<List<EvaluationHomeTopic>>(executors) {
            override fun createCall() = service.getEvaluationTopics().asLiveData()
            override fun saveCallResult(value: List<EvaluationHomeTopic>) = Unit
        }.asLiveData()
    }

    fun getDiscipline(department: String, code: String): LiveData<Resource<EvaluationDiscipline>> {
        return object : NetworkOnlyResource<EvaluationDiscipline>(executors) {
            override fun createCall() = service.getEvaluationDiscipline(department, code).asLiveData()
            override fun saveCallResult(value: EvaluationDiscipline) = Unit
        }.asLiveData()
    }

    fun getTeacherById(teacherId: Long): LiveData<Resource<EvaluationTeacher>> {
        return object : NetworkOnlyResource<EvaluationTeacher>(executors) {
            override fun createCall() = service.getTeacherById(teacherId).asLiveData()
            override fun saveCallResult(value: EvaluationTeacher) = Unit
        }.asLiveData()
    }

    fun getTeacherByName(teacherName: String): LiveData<Resource<EvaluationTeacher>> {
        return object : NetworkOnlyResource<EvaluationTeacher>(executors) {
            override fun createCall() = service.getTeacherByName(teacherName).asLiveData()
            override fun saveCallResult(value: EvaluationTeacher) = Unit
        }.asLiveData()
    }

    fun getQuestionsForTeacher(teacherId: Long): LiveData<Resource<List<Question>>> {
        return object : NetworkOnlyResource<List<Question>>(executors) {
            override fun createCall() = service.getQuestionsForTeachers(teacherId).asLiveData()
            override fun saveCallResult(value: List<Question>) = Unit
        }.asLiveData()
    }

    fun getQuestionsForDiscipline(code: String, department: String): LiveData<Resource<List<Question>>> {
        return object : NetworkOnlyResource<List<Question>>(executors) {
            override fun createCall() = service.getQuestionsForDisciplines(code, department).asLiveData()
            override fun saveCallResult(value: List<Question>) = Unit
        }.asLiveData()
    }

    fun downloadKnowledgeDatabase(): LiveData<Resource<Boolean>> {
        val result = MutableLiveData<Resource<Boolean>>()
        val update = preferences.getLong("_next_evaluation_knowledge_update_02_", 0)
        val calendar = Calendar.getInstance()
        if (update > calendar.timeInMillis) {
            result.value = Resource.success(false)
        } else {
            executors.networkIO().execute {
                result.postValue(Resource.loading(true))
                try {
                    val response = service.getEvaluationSnippetData().execute()
                    if (response.isSuccessful) {
                        val body = response.body()!!
                        database.disciplineServiceDao().insert(body.disciplines)
                        database.teacherServiceDao().insert(body.teachers)
                        database.studentServiceDao().insert(body.students)
                        database.evaluationEntitiesDao().recreate(body)
                        calendar.add(Calendar.DAY_OF_YEAR, 2)
                        preferences.edit().putLong("_next_evaluation_knowledge_update_02_", calendar.timeInMillis).apply()
                        result.postValue(Resource.success(true))
                    } else {
                        result.postValue(Resource.error("Call failed", response.code(), Exception("Call failed")))
                    }
                } catch (t: Throwable) {
                    result.postValue(Resource.error(t.message, 500, t))
                }
            }
        }
        return result
    }

    fun queryEntities(queryProvider: () -> String): Flow<PagingData<EvaluationEntity>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                database.evaluationEntitiesDao().query(queryProvider())
            }
        ).flow
    }

    @AnyThread
    fun answer(data: MutableMap<String, Any?>) {
        executors.networkIO().execute {
            try {
                val response = service.answerQuestion(data).execute()
                if (response.isSuccessful) {
                    Timber.d("Posted answer correctly")
                } else {
                    Timber.d("Answer failed with code ${response.code()}")
                }
            } catch (t: Throwable) {
                Timber.e(t, "error on posting answer")
            }
        }
    }
}
