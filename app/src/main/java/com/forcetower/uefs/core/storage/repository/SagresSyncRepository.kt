package com.forcetower.uefs.core.storage.repository

import android.content.Context
import androidx.annotation.WorkerThread
import com.forcetower.sagres.SagresNavigator
import com.forcetower.sagres.database.model.*
import com.forcetower.sagres.operation.BaseCallback
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.parsers.SagresBasicParser
import com.forcetower.uefs.core.model.unes.*
import com.forcetower.uefs.core.storage.database.UDatabase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SagresSyncRepository @Inject constructor(
    private val context: Context,
    private val database: UDatabase
) {

    @WorkerThread
    fun performSync() {
        val access = database.accessDao().getAccessDirect()
        access?: Timber.d("Access is null, sync will not continue")
        if (access != null) execute(access)
    }

    @WorkerThread
    private fun execute(access: Access) {
        val score = login(access)
        score?: return

        val person = me(score)
        person?: return

        messages(person.id)
        semesters(person.id)
        startPage()
        grades()
    }

    fun login(access: Access): Double? {
        val login = SagresNavigator.instance.login(access.username, access.password)
        when (login.status) {
            Status.SUCCESS -> {
                val score = SagresBasicParser.getScore(login.document)
                Timber.d("Login Completed. Score Parsed: $score")
                return score
            }
            else -> produceErrorMessage(login)
        }
        return null
    }

    private fun me(score: Double): SPerson? {
        val me = SagresNavigator.instance.me()
        when (me.status) {
            Status.SUCCESS -> {
                val person = me.person
                if (person != null) {
                    database.profileDao().insert(person, score)
                    Timber.d("Me completed. Person name: ${person.name}")
                    return person
                } else {
                    Timber.e("Page loaded but API returned invalid types")
                }
            }
            else -> produceErrorMessage(me)
        }
        return null
    }

    @WorkerThread
    private fun messages(userId: Long) {
        val messages = SagresNavigator.instance.messages(userId)
        when (messages.status) {
            Status.SUCCESS -> {
                val values = messages.messages?.map { Message.fromMessage(it, false) }?: emptyList()
                database.messageDao().insertIgnoring(values)
                messagesNotifications()
                Timber.d("Messages completed. Messages size is ${values.size}")
            }
            else -> produceErrorMessage(messages)
        }
    }

    @WorkerThread
    private fun semesters(userId: Long) {
        val semesters = SagresNavigator.instance.semesters(userId)
        when (semesters.status) {
            Status.SUCCESS -> {
                val values = semesters.getSemesters().map { Semester.fromSagres(it) }
                database.semesterDao().insertIgnoring(values)
                Timber.d("Semesters Completed with: ${semesters.getSemesters()}")
            }
            else -> produceErrorMessage(semesters)
        }
    }

    @WorkerThread
    private fun startPage() {
        val start = SagresNavigator.instance.startPage()
        when (start.status) {
            Status.SUCCESS -> {
                defineCalendar(start.calendar)
                defineDisciplines(start.disciplines)
                defineDisciplineGroups(start.groups)
                defineSchedule(start.locations)

                Timber.d("Semesters: ${start.semesters}")
                Timber.d("Disciplines:  ${start.disciplines}")
                Timber.d("Calendar: ${start.calendar}")
            }
            else -> produceErrorMessage(start)
        }
    }

    @WorkerThread
    private fun grades() {
        val grades = SagresNavigator.instance.getCurrentGrades()
        when (grades.status) {
            Status.SUCCESS -> {
                defineSemesters(grades.semesters)
                defineGrades(grades.grades)
                defineFrequency(grades.frequency)

                Timber.d("Grades received: ${grades.grades}")
                Timber.d("Frequency: ${grades.frequency}")
                Timber.d("Semesters: ${grades.semesters}")

                gradesNotifications()
                frequencyNotifications()

                Timber.d("Completed!")
            }
            else -> produceErrorMessage(grades)
        }
    }

    private fun frequencyNotifications() {
        //TODO Implement frequency notifications
    }

    private fun gradesNotifications() {
        //TODO Implement grades notifications
    }

    @WorkerThread
    private fun messagesNotifications() {
        val messages = database.messageDao().getNewMessages()
        database.messageDao().setAllNotified()
        messages.forEach { it.notify(context) }
    }

    @WorkerThread
    private fun defineFrequency(frequency: List<SDisciplineMissedClass>?) {
        if (frequency == null) return
        database.classAbsenceDao().putAbsences(frequency)
    }

    @WorkerThread
    private fun defineGrades(grades: List<SGrade>) {
        database.gradesDao().putGrades(grades)
    }

    @WorkerThread
    private fun defineSemesters(semesters: List<Pair<Long, String>>) {
        semesters.forEach {
            val semester = Semester(sagresId = it.first, name = it.second, codename = it.second)
            database.semesterDao().insertIgnoring(semester)
        }
    }

    @WorkerThread
    private fun defineSchedule(locations: List<SDisciplineClassLocation>?) {
        if (locations == null) return
        database.classLocationDao().putSchedule(locations)
    }

    @WorkerThread
    private fun defineDisciplineGroups(groups: List<SDisciplineGroup>) {
        val values = ArrayList<ClassGroup>()
        groups.forEach {
            val group = database.classGroupDao().insert(it)
            values.add(group)
        }
        database.classStudentDao().joinGroups(values)
    }

    @WorkerThread
    private fun defineDisciplines(disciplines: List<SDiscipline>) {
        val values = disciplines.map { Discipline.fromSagres(it) }
        database.disciplineDao().insert(values)
        disciplines.forEach { database.classDao().insert(it) }
    }

    @WorkerThread
    private fun defineCalendar(calendar: List<SCalendar>?) {
        val values = calendar?.map { CalendarItem.fromSagres(it) }
        database.calendarDao().deleteAndInsert(values)
    }

    private fun produceErrorMessage(callback: BaseCallback<*>) {
        Timber.e("Failed executing with status ${callback.status} and throwable message [${callback.throwable?.message}]")
    }

}