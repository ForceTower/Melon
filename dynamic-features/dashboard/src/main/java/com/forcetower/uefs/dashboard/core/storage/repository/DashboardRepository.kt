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

package com.forcetower.uefs.dashboard.core.storage.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.SStudent
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.database.aggregation.ClassLocationWithData
import com.forcetower.uefs.core.work.affinity.AnswerAffinityWorker
import dagger.Reusable
import java.util.Calendar
import javax.inject.Inject

@Reusable
class DashboardRepository @Inject constructor(
    private val executors: AppExecutors,
    private val database: UDatabase,
    private val context: Context
) {
    fun getAccount(): LiveData<Account?> {
        return database.accountDao().getAccountNullable()
    }

    fun getCurrentClass(): LiveData<ClassLocationWithData?> {
        val calendar = Calendar.getInstance()
        val dayInt = calendar.get(Calendar.DAY_OF_WEEK)
        val currentTimeInt = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return database.classLocationDao().getCurrentClass(dayInt, currentTimeInt)
    }

    fun getLastMessage(): LiveData<Message?> {
        return database.messageDao().getLastMessage()
    }

    fun getStudentMe(): LiveData<SStudent> {
        return database.studentServiceDao().getMeStudent()
    }

    fun getAffinityQuestions() = database.affinityQuestion().getUnansweredQuestions()

    fun scheduleAnswerAffinity(questionId: Long, studentId: Long) {
        executors.diskIO().execute { database.affinityQuestion().answerQuestion(questionId) }
        AnswerAffinityWorker.createWorker(context, questionId, studentId)
    }
}
