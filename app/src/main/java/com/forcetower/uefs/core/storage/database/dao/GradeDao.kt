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
import com.forcetower.sagres.database.model.SagresGrade
import com.forcetower.sagres.database.model.SagresGradeInfo
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.aggregation.GradeWithClassStudent
import dev.forcetower.breaker.model.ClassEvaluation
import timber.log.Timber
import java.time.ZonedDateTime

@Dao
abstract class GradeDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(grade: Grade): Long

    @Update(onConflict = IGNORE)
    abstract fun update(grade: Grade)

    @Query("SELECT * FROM Grade")
    abstract fun getAllGradesDirect(): List<Grade>

    @Transaction
    @Query("SELECT * FROM Grade WHERE notified = 1")
    abstract fun getCreatedGradesDirect(): List<GradeWithClassStudent>

    @Transaction
    @Query("SELECT * FROM Grade WHERE notified = 2")
    abstract fun getDateChangedGradesDirect(): List<GradeWithClassStudent>

    @Transaction
    @Query("SELECT * FROM Grade WHERE notified = 3")
    abstract fun getPostedGradesDirect(): List<GradeWithClassStudent>

    @Transaction
    @Query("SELECT * FROM Grade WHERE notified = 4")
    abstract fun getChangedGradesDirect(): List<GradeWithClassStudent>

    @Query("UPDATE Grade SET notified = 0")
    abstract fun markAllNotified()

    @Transaction
    open fun putGrades(grades: List<SagresGrade>, notify: Boolean = true) {
        grades.forEach {
            val split = it.discipline.split("-")
            val code = split[0].trim()
            var nameOne = split[1]
            if (split.size > 2) {
                val created = split.subList(1, split.size).joinTo(StringBuffer(), separator = "-").trim().toString()
                Timber.d("created is $created")
                nameOne = created
            }

            val finalScore = it.finalScore
                .replace(",", ".")
                .replace("-", "")
                .replace("*", "")

            val partialMean = it.partialMean
                .replace(",", ".")
                .replace("-", "")
                .replace("*", "")
            val partialScore = partialMean.toDoubleOrNull()
            val score = finalScore.toDoubleOrNull()

            var clazz = getClass(code, it.semesterId)
            if (clazz != null) {
                // this is the default scenario, class already exists and we are good to go
                if (clazz.scheduleOnly) updateClassScheduleOnly(clazz.uid, false)
                if (score != null) updateClassScore(clazz.uid, score)
                if (partialScore != null) updateClassPartialScore(clazz.uid, partialScore)

                prepareInsertion(clazz, it, notify)
            } else {
                // this is the less optimal scenario, you have the grades, but nothing else.
                // the app must create all now!
                Timber.d("<grades_clazz_404> :: Clazz not found for ${code}_${it.semesterId}")
                val index = nameOne.lastIndexOf("(")
                val realIndex = if (index == -1) nameOne.length else index
                val name = nameOne.substring(0, realIndex).trim()

                var discipline = selectDisciplineDirect(code)
                Timber.d("code: $code -> disciplineId $discipline")
                val semester = selectSemesterDirect(it.semesterId)

                if (discipline == null) {
                    val fakeDiscipline = Discipline(name = name, code = code, credits = 0)
                    val id = insertDiscipline(fakeDiscipline)
                    Timber.d("Id discipline inserted $id")
                    fakeDiscipline.uid = id
                    discipline = fakeDiscipline
                }

                Timber.d("Discipline after all $discipline")
                if (semester != null && discipline.uid > 0) {
                    clazz = getClazz(discipline.uid, semester.uid)
                    if (clazz == null) {
                        clazz = Class(disciplineId = discipline.uid, semesterId = semester.uid)
                        Timber.d("disciplineId ${discipline.uid} semesterId ${semester.uid}")
                        val id = insertClass(clazz)
                        clazz.uid = id
                    }
                    if (clazz.uid > 0) {
                        if (score != null)
                            updateClassScore(clazz.uid, score)
                        if (partialScore != null)
                            updateClassPartialScore(clazz.uid, partialScore)

                        prepareInsertion(clazz, it, notify)
                    }
                }
            }
        }
        grades.associateBy { it.semesterId }.keys.mapNotNull { getSemesterId(it) }.forEach {
            Timber.d("delete stales from ${it.name}")
            val affected = deleteStaleGrades(it.uid)
            Timber.d("Rows affected $affected")
        }
    }

    @Query("SELECT * FROM Class WHERE discipline_id = :disciplineId AND semester_id = :semesterId")
    abstract fun getClazz(disciplineId: Long, semesterId: Long): Class?

    @Query("SELECT c.* FROM Class c, Discipline d, Semester s WHERE c.semester_id = s.uid AND c.discipline_id = d.uid AND LOWER(d.code) = LOWER(:code) AND s.sagres_id = :semester")
    protected abstract fun getClass(code: String, semester: Long): Class?

    private fun prepareInsertion(clazz: Class, it: SagresGrade, notify: Boolean) {
        // This is used to select the best grade when multiple ones with same identifier is found

        // This part filters out the useless grades we get from html
        val values = HashMap<String, SagresGradeInfo>()
        it.values.forEach { g ->
            var grade = values["${g.grouping}<><>${g.name}"]
            if (grade == null) {
                grade = g
            } else {
                if (g.hasGrade()) grade = g
                else if (g.hasDate() && grade.hasDate() && g.date != grade.date) grade = g
                else Timber.d("This grade was ignored ${g.name}_${g.grade}")
            }
            values["${g.grouping}<><>${g.name}"] = grade
        }

        // this actually inserts stuff into the database
        values.values.forEach { i ->
            val grade = getNamedGradeDirect(clazz.uid, i.name, i.grouping)
            if (grade == null) {
                val notified = if (i.hasGrade()) 3 else 1
                insert(
                    Grade(
                        classId = clazz.uid,
                        name = i.name,
                        date = i.date,
                        notified = if (notify) notified else 0,
                        grade = i.grade,
                        grouping = i.grouping,
                        groupingName = i.groupingName
                    )
                )
            } else {
                var shouldUpdate = true
                if (grade.hasGrade() && i.hasGrade() && grade.grade != i.grade) {
                    grade.notified = 4
                    grade.grade = i.grade
                    grade.date = i.date
                } else if (!grade.hasGrade() && i.hasGrade()) {
                    grade.notified = 3
                    grade.grade = i.grade
                    grade.date = i.date
                } else if (!grade.hasGrade() && !i.hasGrade() && grade.date != i.date) {
                    grade.notified = 2
                    grade.date = i.date
                } else {
                    shouldUpdate = false
                    Timber.d("No changes detected between ${grade.name} ${grade.grouping} and ${i.name} ${i.grouping}")
                }

                if (grade.groupingName != i.groupingName) {
                    shouldUpdate = true
                    grade.groupingName = i.groupingName
                }

                grade.notified = if (notify) grade.notified else 0
                if (shouldUpdate) update(grade)
            }
        }
    }

    @Transaction
    open suspend fun putGradesNewWay(classId: Long, evaluations: List<ClassEvaluation>, notify: Boolean = true) {
        evaluations.forEach { evaluation ->
            val grades = evaluation.grades
            val named = grades.groupBy { it.name }
            val remapped = named.entries.map { entry ->
                if (entry.value.size == 1) {
                    entry.value[0]
                } else {
                    // Some disciplines still shows more than one practice.
                    // this could be removed iof we show all with the same name to the user,
                    // but that would trigger notifications to classes we don't need.
                    // There are 4 solutions:
                    // - remove date changes notifications, and show everything
                    // - remove date changes notifications, show only one grade, but date might be incorrect
                    // - show everything
                    // - show only one, but date might be incorrect
                    // for now, UNES will use option 4, prioritizing earlier dates (better study for early test)
                    entry.value.minByOrNull {
                        when {
                            it.value != null -> Int.MIN_VALUE
                            it.date != null -> ZonedDateTime.parse(it.date!!).toEpochSecond().toInt()
                            else -> Int.MAX_VALUE
                        }
                    }!!
                }
            }

            remapped.forEach { grade ->
                Timber.d("Attempt to insert ${evaluation.name} ${grade.name} ${grade.value}")
                val current = getNamedGradeDirect(classId, "${grade.nameShort} - ${grade.name}", evaluation.name.hashCode())
                Timber.d("Attempt to override ${current?.name} ${current?.groupingName} ${current?.grade}")
                Timber.d("Current $current")
                if (current == null) {
                    val notified = if (grade.hasGrade()) 3 else 1
                    insert(
                        Grade(
                            classId = classId,
                            name = "${grade.nameShort} - ${grade.name}",
                            notified = if (notify) notified else 0,
                            grade = grade.value?.toString(),
                            grouping = evaluation.name.hashCode(),
                            groupingName = evaluation.name ?: "Notas",
                            date = grade.date
                        )
                    )
                } else {
                    var shouldUpdate = true
                    val score = grade.value?.toString() ?: ""
                    if (current.hasGrade() && grade.hasGrade() && score != current.grade) {
                        current.notified = 4
                        current.grade = score
                        current.date = grade.date
                    } else if (!current.hasGrade() && grade.hasGrade()) {
                        current.notified = 3
                        current.grade = score
                        current.date = grade.date
                    } else if (!current.hasGrade() && !grade.hasGrade() && current.date != grade.date) {
                        current.notified = 2
                        current.date = grade.date
                    } else {
                        shouldUpdate = false
                        Timber.d("No changes detected between ${current.name} ${current.grouping} and ${grade.name} ${evaluation.name.hashCode()}")
                    }

                    if (current.groupingName != evaluation.name) {
                        shouldUpdate = true
                        current.groupingName = evaluation.name ?: "Notas"
                    }

                    current.notified = if (notify) current.notified else 0
                    if (shouldUpdate) update(current)
                }
            }
        }
    }

    @Query("UPDATE Class SET schedule_only = :scheduleOnly WHERE uid = :classId")
    abstract fun updateClassScheduleOnly(classId: Long, scheduleOnly: Boolean)

    @Query("UPDATE Class SET partial_score = :partialScore WHERE uid = :classId")
    protected abstract fun updateClassPartialScore(classId: Long, partialScore: Double)

    @Query("UPDATE Class SET final_score = :score WHERE uid = :classId")
    protected abstract fun updateClassScore(classId: Long, score: Double)

    @Query("SELECT * FROM Grade WHERE class_id = :classId AND name = :name AND grouping = :grouping")
    protected abstract fun getNamedGradeDirect(classId: Long, name: String, grouping: Int): Grade?

    @Query("SELECT * FROM Profile WHERE me = 1")
    protected abstract fun getMeProfile(): Profile

    @Query("SELECT * FROM Discipline WHERE LOWER(code) = LOWER(:code)")
    protected abstract fun selectDisciplineDirect(code: String): Discipline?

    @Query("SELECT * FROM Semester WHERE sagres_id = :sagresId")
    protected abstract fun selectSemesterDirect(sagresId: Long): Semester?

    @Query("DELETE FROM Grade WHERE groupingName = 'UNES_Group_0' AND class_id IN (SELECT c.uid FROM Class AS c WHERE c.semester_id = :semesterId)")
    protected abstract fun deleteStaleGrades(semesterId: Long): Int

    @Query("SELECT * FROM Semester WHERE sagres_id = :sagresSemesterId")
    protected abstract fun getSemesterId(sagresSemesterId: Long): Semester?

    @Insert(onConflict = IGNORE)
    protected abstract fun insertDiscipline(discipline: Discipline): Long

    @Insert(onConflict = IGNORE)
    protected abstract fun insertClass(clazz: Class): Long
}
