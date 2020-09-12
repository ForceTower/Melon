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

package com.forcetower.uefs.core.storage.repository.cloud

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.service.AffinityQuestionAnswer
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AffinityQuestionRepository @Inject constructor(
    private val executors: AppExecutors,
    private val database: UDatabase,
    private val service: UService
) {
    @AnyThread
    fun getAffinityQuestionsAsync() {
        executors.networkIO().execute {
            getAffinityQuestions()
        }
    }

    @WorkerThread
    fun getAffinityQuestions() {
        try {
            val response = service.affinityQuestions().execute()
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) {
                    database.affinityQuestion().insert(data)
                } else {
                    Timber.d("A null response... Figure..")
                }
            } else {
                Timber.d("A failed response with code ${response.code()}")
            }
        } catch (error: Throwable) {
            Timber.d(error, "Exception (:")
        }
    }

    fun answerAffinityQuestion(questionId: Long, studentId: Long): Boolean {
        return try {
            val response = service.answerAffinity(AffinityQuestionAnswer(questionId, studentId)).execute()
            Timber.d("Response result: ${response.isSuccessful} ${response.code()}")
            if (response.isSuccessful) {
                database.affinityQuestion().markSynced(questionId)
            }
            return response.isSuccessful
        } catch (error: Throwable) {
            Timber.d(error, "Error answering")
            false
        }
    }
}
