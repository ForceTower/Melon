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

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.forcetower.sagres.database.model.SagresPerson
import com.forcetower.sagres.operation.Callback
import com.forcetower.sagres.operation.Status
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.task.definers.DisciplinesProcessor
import com.forcetower.uefs.core.task.definers.MessagesProcessor
import com.forcetower.uefs.core.task.definers.SemestersProcessor
import dev.forcetower.breaker.Orchestra
import dev.forcetower.breaker.model.Authorization
import dev.forcetower.breaker.model.Person
import dev.forcetower.breaker.result.Outcome
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class SnowpiercerLoginRepository @Inject constructor(
    private val client: OkHttpClient,
    private val database: UDatabase,
    private val context: Context,
    private val executors: AppExecutors,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    @Named("webViewUA") private val agent: String
) {
    val currentStep: MutableLiveData<LoginSagresRepository.Step> = MutableLiveData()

    fun connect(username: String, password: String, deleteDatabase: Boolean = true) = flow {
        val orchestra = Orchestra.Builder().userAgent(agent).client(client).build()
        LoginSagresRepository.resetSteps()
        if (deleteDatabase) {
            currentStep.postValue(LoginSagresRepository.createStep(R.string.step_delete_database))
            database.accessDao().deleteAll()
            database.messageDao().deleteAll()
            database.accessTokenDao().deleteAll()
            database.calendarDao().delete()
            database.profileDao().deleteMe()
            database.semesterDao().deleteAll()
        }

        orchestra.setAuthorization(Authorization(username, password))

        val login = orchestra.login()
        if (login is Outcome.Error) {
            if (login.code == 401) {
                emit(Callback.Builder(Status.INVALID_LOGIN).code(401).build())
            } else emit(produceErrorMessage(login))
            return@flow
        }

        database.accessDao().insert(username, password)

        val person = (login as Outcome.Success).value
        executors.others().execute {
            firebaseAuthRepository.loginToFirebase(
                SagresPerson(
                    person.id,
                    person.name,
                    person.name,
                    person.cpf,
                    person.email
                ),
                Access(username = username, password = password),
                true
            )
        }

        val localProfileId = defineUser(person)

        LoginSagresRepository.createStep(0)
        currentStep.postValue(LoginSagresRepository.createStep(R.string.step_fetching_messages))
        val messagesOutcome = orchestra.messages(person.id)
        (messagesOutcome as? Outcome.Success)?.let { success ->
            val page = success.value
            MessagesProcessor(page, database, context, true).execute()
        }

        if (messagesOutcome is Outcome.Error) {
            Timber.d("Messages error code: ${messagesOutcome.code}")
            messagesOutcome.error.printStackTrace()
        }

        LoginSagresRepository.createStep(0)
        currentStep.postValue(LoginSagresRepository.createStep(R.string.step_fetching_semesters))
        val semestersOutcome = orchestra.semesters(person.id)
        val currentSemester = (semestersOutcome as? Outcome.Success)?.let { success ->
            val semesters = success.value
            SemestersProcessor(database, semesters).execute()

            val current = semesters.maxByOrNull { it.start }
            current
        }

        if (semestersOutcome is Outcome.Error) {
            Timber.d("Semester error code: ${semestersOutcome.code}")
            semestersOutcome.error.printStackTrace()
        }

        currentStep.postValue(LoginSagresRepository.createStep(R.string.step_fetching_grades))
        currentSemester?.let { semester ->
            val gradesOutcome = orchestra.grades(person.id, semester.id)
            (gradesOutcome as? Outcome.Success)?.let { success ->
                val disciplines = success.value
                val currentSemesterIns = database.semesterDao().getSemesterDirect(semester.id)!!
                DisciplinesProcessor(context, database, disciplines, currentSemesterIns.uid, localProfileId, false).execute()
            }

            if (gradesOutcome is Outcome.Error) {
                Timber.d("Grades error code: ${gradesOutcome.code}")
                gradesOutcome.error.printStackTrace()
            }
        }

        emit(Callback.Builder(Status.SUCCESS).build())
    }

    private suspend fun defineUser(person: Person): Long {
        return database.profileDao().insert(person)
    }

    private fun produceErrorMessage(outcome: Outcome.Error<*>): Callback {
        return Callback.Builder(Status.RESPONSE_FAILED)
            .code(outcome.code)
            .throwable(outcome.error)
            .build()
    }
}
