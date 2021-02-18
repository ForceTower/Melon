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

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.location.Location
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.model.service.AchDistance
import com.forcetower.uefs.core.model.service.Achievement
import com.forcetower.uefs.core.model.unes.ClassLocation
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.feature.shared.extensions.generateCalendarFromHour
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.set

class AdventureRepository @Inject constructor(
    private val database: UDatabase,
    private val executors: AppExecutors,
    private val auth: FirebaseAuth,
    private val preferences: SharedPreferences,
    private val locations: AchLocationsRepository,
    private val service: UService
) {

    @AnyThread
    fun checkServerAchievements(): LiveData<List<Achievement>> {
        val data = MutableLiveData<List<Achievement>>()
        executors.networkIO().execute {
            try {
                val response = service.getServerAchievements().execute()
                if (response.isSuccessful) {
                    val value = response.body()?.data ?: emptyList()
                    data.postValue(value)
                } else {
                    data.postValue(emptyList())
                }
            } catch (error: Throwable) {
                data.postValue(emptyList())
            }
        }
        return data
    }

    @AnyThread
    fun matchesAnyAchievement(location: Location?): List<AchDistance> {
        return locations.onReceiveLocation(location)
    }

    @AnyThread
    fun checkAchievements(): LiveData<Map<Int, Int>> {
        val data = MutableLiveData<Map<Int, Int>>()
        executors.diskIO().execute {
            val list = internalCheckAchievements()
            data.postValue(list)
        }
        return data
    }

    @AnyThread
    fun justCheckAchievements(): LiveData<Map<Int, Int>> {
        val data = MutableLiveData<Map<Int, Int>>()
        executors.diskIO().execute {
            val map = HashMap<Int, Int>()
            performCheckAchievements(map)
            data.postValue(map)
        }
        return data
    }

    @SuppressLint("UseSparseArrays")
    @WorkerThread
    private fun internalCheckAchievements(): Map<Int, Int> {
        val data = HashMap<Int, Int>()

        auth.currentUser ?: return data

        performCheckAchievements(data)
        return data
    }

    @WorkerThread
    fun performCheckAchievements(data: HashMap<Int, Int>) {
        val semesters = database.semesterDao().getSemestersDirect()
        unlockSemesterBased(semesters, data)
        val schedule = database.classLocationDao().getCurrentScheduleDirect()
        unlockScheduleBased(schedule, data)

        val darkTheme = preferences.getBoolean("ach_night_mode_enabled", false)
        if (darkTheme) {
            data[R.string.achievement_escuridao] = -1
        }

        data[R.string.achievement_atualizado] = -1
    }

    private fun unlockSemesterBased(semesters: List<Semester>, data: HashMap<Int, Int>) {
        val profile = database.profileDao().selectMeDirect()
        val score = profile?.score ?: profile?.calcScore ?: -1.0

        if (semesters.size > 5 && score >= 7)
            data[R.string.achievement_sobrevivente] = -1

        if (semesters.size > 4) data[R.string.achievement_veterano] = -1
        if (semesters.size > 7) data[R.string.achievement_e_ai_forma_quando] = -1
        if (semesters.size > 1) {
            data[R.string.achievement_pseudoveterano] = -1

            val sorted = if (semesters.all { it.start != null }) {
                semesters.sortedBy { it.start }
            } else {
                semesters.sortedBy { it.sagresId }
            }.subList(0, semesters.size - 1)

            var noFinalCount = 0
            var introduction = 0

            // Only previous semesters
            sorted.forEach { semester ->
                val start = semester.start
                val end = semester.end
                if (start != null && end != null) {
                    val diff = end - start
                    val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
                    if (days <= 180) data[R.string.achievement_semestre_de_6_meses] = -1

                    val calendarStart = Calendar.getInstance().apply { timeInMillis = start }.get(Calendar.YEAR)
                    val calendarEnd = Calendar.getInstance().apply { timeInMillis = end }.get(Calendar.YEAR)

                    if (calendarStart != calendarEnd)
                        data[R.string.achievement_semestre_da_virada] = -1
                }

                var final = false
                var mechanics = true
                var hours = 0
                val classes = database.classDao().getClassesWithGradesFromSemesterDirect(semester.uid)
                var valid = false
                classes.forEach { clazz ->
                    val points = clazz.clazz.finalScore ?: 0.0
                    val credits = clazz.discipline.credits

                    if (points < 7 && points >= 0) final = true
                    if (points == 10.0) data[R.string.achievement_mdia_10] = -1
                    if (points >= 5 && points < 7) data[R.string.achievement_luta_at_o_fim] = -1
                    if (points == 5.0) data[R.string.achievement_quase] = -1
                    if (points in 9.5..9.9) data[R.string.achievement_to_perto_mas_to_longe] = -1
                    if (points < 8) mechanics = false

                    val teacher = Constants.HARD_DISCIPLINES[clazz.discipline.code.toUpperCase(Locale.getDefault())]
                    if (teacher != null && points >= 5) {
                        if (teacher == "__ANY__") {
                            data[R.string.achievement_vale_das_sombras] = -1
                        } else {
                            clazz.groups.forEach { group ->
                                val current = group.teacher
                                if (current != null && teacher.equals(current, ignoreCase = true)) {
                                    data[R.string.achievement_vale_das_sombras] = -1
                                }
                            }
                        }
                    }

                    clazz.grades.forEach { grade ->
                        valid = true
                        val number = grade.gradeDouble() ?: -1
                        if (number == 7) data[R.string.achievement_medocre] = -1
                        if (number == 10) data[R.string.achievement_achei_fcil] = -1
                    }

                    if (clazz.grades.size == 3) {
                        val one = clazz.grades[0].gradeDouble()
                        val two = clazz.grades[1].gradeDouble()
                        val thr = clazz.grades[2].gradeDouble()

                        if (one != null && two != null && thr != null) {
                            if (one < 5 && two > 8.5 && thr > 8.5)
                                data[R.string.achievement_agora_todas_as_peas_se_encaixaram] = -1
                            if (one == 7.0 && two == 7.0 && thr == 7.0)
                                data[R.string.achievement_jackpot]
                        }
                    }

                    val absences = database.classAbsenceDao().getAbsenceFromClassDirect(clazz.clazz.uid)
                    if (absences.isEmpty()) data[R.string.achievement_eu_estou_sempre_l] = -1
                    if (clazz.clazz.missedClasses >= credits / 4) data[R.string.achievement_nunca_nem_vi] = -1

                    val name = clazz.discipline.name
                    if (name.matches("(?i)(.*)introdu([cç])([aã])o(.*)".toRegex())) {
                        introduction++
                    } else if (name.matches("(?i)(.*)int(r)?\\.(.*)".toRegex())) {
                        introduction++
                    }

                    hours += credits
                }

                if (!final && valid) {
                    data[R.string.achievement_semestre_limpo] = -1
                    noFinalCount++
                } else {
                    noFinalCount = 0
                }

                if (noFinalCount == 3) data[R.string.achievement_killing_spree] = -1

                mechanics = mechanics and (classes.size >= 4)
                if (mechanics) data[R.string.achievement_mecanizou_todo] = -1

                if (hours >= 480) data[R.string.achievement_me_empresta_o_seu_viratempo] = -1
                else if (hours <= 275) data[R.string.achievement_engatinhando] = -1
            }

            data[R.string.achievement_introduo_a_introdues] = introduction
        }

        // Takes only the current semester
        val current = if (semesters.all { it.start != null }) {
            semesters.maxByOrNull { it.start!! }
        } else {
            semesters.maxByOrNull { it.sagresId }
        }

        if (current != null) {
            var hours = 0
            val classes = database.classDao().getClassesWithGradesFromSemesterDirect(current.uid)
            classes.forEach { clazz ->
                val points = clazz.clazz.finalScore ?: -1.0
                val credits = clazz.discipline.credits

                hours += credits

                if (points == 10.0) data[R.string.achievement_mdia_10] = -1
                if (points >= 5 && points < 7) data[R.string.achievement_luta_at_o_fim] = -1
                if (points == 5.0) data[R.string.achievement_quase] = -1
                if (points in 9.5..9.9) data[R.string.achievement_to_perto_mas_to_longe] = -1

                clazz.grades.forEach { grade ->
                    val number = grade.gradeDouble() ?: -1
                    if (number == 7) data[R.string.achievement_medocre] = -1
                    if (number == 10) data[R.string.achievement_achei_fcil] = -1
                }

                if (clazz.grades.size == 3) {
                    val one = clazz.grades[0].gradeDouble()
                    val two = clazz.grades[1].gradeDouble()
                    val thr = clazz.grades[2].gradeDouble()

                    if (one != null && two != null && thr != null) {
                        if (one < 5 && two > 8.5 && thr > 8.5)
                            data[R.string.achievement_agora_todas_as_peas_se_encaixaram] = -1
                        if (one == 7.0 && two == 7.0 && thr == 7.0)
                            data[R.string.achievement_jackpot]
                    }
                }
            }

            if (hours >= 480) data[R.string.achievement_me_empresta_o_seu_viratempo] = -1
            else if (hours <= 275) data[R.string.achievement_engatinhando] = -1
        }
    }

    private fun unlockScheduleBased(schedule: List<ClassLocation>, data: HashMap<Int, Int>) {
        val day = schedule.groupBy { it.day }
        day.entries.forEach { group ->
            var minutes = 0
            group.value.forEach { location ->
                val start = location.startsAt.generateCalendarFromHour()?.timeInMillis
                val end = location.endsAt.generateCalendarFromHour()?.timeInMillis
                if (start != null && end != null) {
                    val diff = end - start
                    minutes += TimeUnit.MINUTES.convert(diff, TimeUnit.MILLISECONDS).toInt()
                }
            }
            if (minutes >= 480) data[R.string.achievement_maratonista] = -1
        }

        var mod1 = false
        var mod7 = false
        schedule.forEach { location ->
            val mod = location.modulo ?: "unknown"
            if (!mod1) mod1 = mod.equals("Módulo 1", ignoreCase = true) || mod.equals("Modulo 1", ignoreCase = true)
            if (!mod7) mod7 = mod.equals("Módulo 7", ignoreCase = true) || mod.equals("Modulo 7", ignoreCase = true)
        }

        if (mod1 && mod7) data[R.string.achievement_dora_a_exploradora] = -1
    }
}
