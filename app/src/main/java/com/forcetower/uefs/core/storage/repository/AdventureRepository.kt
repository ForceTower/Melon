/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.core.storage.repository

import android.content.SharedPreferences
import android.location.Location
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.ClassLocation
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.feature.shared.extensions.generateCalendarFromHour
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import kotlin.collections.set

class AdventureRepository @Inject constructor(
    private val database: UDatabase,
    private val executors: AppExecutors,
    @Named(Profile.COLLECTION) private val collection: CollectionReference,
    private val auth: FirebaseAuth,
    private val preferences: SharedPreferences,
    private val locations: AchLocationsRepository
) {

    @AnyThread
    fun matchesAnyAchievement(location: Location): Int? {
        return locations.onReceiveLocation(location)
    }

    @AnyThread
    fun checkAchievements(email: String? = null): LiveData<Map<Int, Int>> {
        val data = MutableLiveData<Map<Int, Int>>()
        executors.diskIO().execute {
            val list = internalCheckAchievements(email)
            data.postValue(list)
        }
        return data
    }

    @WorkerThread
    private fun internalCheckAchievements(email: String?): Map<Int, Int> {
        val data = HashMap<Int, Int>()

        val user = auth.currentUser ?: return data
        val id = user.uid

        try {
            val snapshot = Tasks.await(collection.document(id).get())
            val games = snapshot?.data?.get("adventure") as? String
            if (games == null && email != null) {
                Timber.d("Setting up account")
                Tasks.await(collection.document(id).set(mapOf("adventure" to email), SetOptions.merge()))
                performCheckAchievements(data)
            } else if (games == email) {
                performCheckAchievements(data)
            } else {
                Timber.d("Invalid combination of $email and ${user.email}")
            }
        } catch (exception: Exception) {
            Timber.d("Ignored exception: ${exception.message}")
        }

        return data
    }

    @WorkerThread
    private fun performCheckAchievements(data: HashMap<Int, Int>) {
        val semesters = database.semesterDao().getSemestersDirect()
        unlockSemesterBased(semesters, data)
        val schedule = database.classLocationDao().getCurrentScheduleDirect()
        unlockScheduleBased(schedule, data)

        val old = preferences.getBoolean("old_fella", false)
        data[R.string.achievement_atualizado] = -1
    }

    private fun unlockSemesterBased(semesters: Collection<Semester>, data: HashMap<Int, Int>) {
        val profile = database.profileDao().selectMeDirect()
        val score = profile?.score ?: profile?.calcScore ?: -1.0

        if (semesters.size > 5 && score >= 7)
            data[R.string.achievement_sobrevivente] = -1

        if (semesters.size > 4) data[R.string.achievement_veterano] = -1
        if (semesters.size > 7) data[R.string.achievement_e_ai_forma_quando] = -1
        if (semesters.size > 1) {
            data[R.string.achievement_pseudoveterano] = -1

            val sorted = semesters.sortedBy { it.sagresId }.subList(0, semesters.size - 1)
            var noFinalCount = 0
            var introduction = 0
            var accumulatedMean = 0.0
            var accumulatedHours = 0

            sorted.forEach { semester ->
                val start = semester.start
                val end = semester.end
                if (start != null && end != null) {
                    val diff = end - start
                    val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
                    if (days < 180) data[R.string.achievement_semestre_de_6_meses] = -1

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
                    val points = clazz.clazz.finalScore ?: -1.0
                    val credits = clazz.discipline().credits

                    if (points >= 0) {
                        accumulatedMean += points * credits
                        accumulatedHours += credits
                    }

                    if (points < 7 && points >= 0) final = true
                    if (points == 10.0) data[R.string.achievement_mdia_10] = -1
                    if (points >= 5 && points < 7) data[R.string.achievement_luta_at_o_fim] = -1
                    if (points == 5.0) data[R.string.achievement_quase] = -1
                    if (points in 9.5..9.9) data[R.string.achievement_to_perto_mas_to_longe] = -1
                    if (points < 8) mechanics = false

                    clazz.grades.forEach { grade ->
                        valid = true
                        val number = grade.gradeDouble() ?: -1
                        if (number == 7) data[R.string.achievement_medocre] = -1
                        if (number == 10) data[R.string.achievement_achei_fcil] = -1
                    }

                    if (clazz.grades.size == 3) {
                        val one = clazz.grades[0].gradeDouble()
                        val two = clazz.grades[0].gradeDouble()
                        val thr = clazz.grades[0].gradeDouble()

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

                    val name = clazz.discipline().name
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

            val calcScore = accumulatedMean / accumulatedHours
            Timber.d("Score calculated is: $calcScore")
            database.profileDao().updateCalculatedScore(calcScore)
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
