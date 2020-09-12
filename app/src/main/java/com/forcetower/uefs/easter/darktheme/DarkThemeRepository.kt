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

package com.forcetower.uefs.easter.darktheme

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.api.DarkInvite
import com.forcetower.uefs.core.model.api.DarkUnlock
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.easter.twofoureight.tools.ScoreKeeper
import com.forcetower.uefs.feature.shared.extensions.generateCalendarFromHour
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DarkThemeRepository @Inject constructor(
    private val preferences: SharedPreferences,
    private val context: Context,
    private val executors: AppExecutors,
    private val database: UDatabase,
    private val service: UService
) {

    fun getPreconditions(): LiveData<List<Precondition>> {
        val result = MutableLiveData<List<Precondition>>()
        executors.diskIO().execute {
            val precondition1 = create2048Precondition()
            val precondition2 = createLocationPrecondition()
            val precondition3 = createHoursPrecondition()
            val list = listOf(precondition1, precondition2, precondition3)
            executors.others().execute {
                sendInfoToServer(list)
            }
            result.postValue(list)
        }
        return result
    }

    @WorkerThread
    private fun sendInfoToServer(list: List<Precondition>) {
        val completed = list.filter { it.completed }
        val completedSize = completed.size
        val account = database.accountDao().getAccountDirect()
        if (completed.isEmpty()) return

        if (account != null) {
            try {
                service.requestDarkThemeUnlock(DarkUnlock(completedSize)).execute()
            } catch (throwable: Throwable) {
                Timber.e(throwable)
            }
        }
    }

    private fun create2048Precondition(): Precondition {
        val the2048score = context.getSharedPreferences(ScoreKeeper.PREFERENCES, Context.MODE_PRIVATE)
            .getLong(ScoreKeeper.HIGH_SCORE, 0)
        Timber.d("2048 score: $the2048score")
        return Precondition(context.getString(R.string.precondition_1), context.getString(R.string.precondition_1_desc), the2048score >= 50000)
    }

    @WorkerThread
    private fun createLocationPrecondition(): Precondition {
        val bigTray = preferences.getBoolean("ach_dora_big_tray", false)
        val library = preferences.getBoolean("ach_dora_library", false)
        val zoology = preferences.getBoolean("ach_dora_zoology", false)
        val hogwarts = preferences.getBoolean("ach_dora_hogwarts", false)
        val module1 = preferences.getBoolean("ach_dora_mod1", false)
        val module7 = preferences.getBoolean("ach_dora_mod7", false)
        val management = preferences.getBoolean("ach_dora_management", false)
        val all = bigTray and library and zoology and hogwarts and module1 and module7 and management

        val schedule = database.classLocationDao().getCurrentScheduleDirect()
        var eightHours = false
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
            if (minutes >= 480) eightHours = true
        }

        return Precondition(context.getString(R.string.precondition_2), context.getString(R.string.precondition_2_desc), all && eightHours)
    }

    @WorkerThread
    private fun createHoursPrecondition(): Precondition {
        val list = database.classDao().getAllDirect()
        val credits = list.asSequence().map { it.discipline.credits }.sum()
        Timber.d("Credits: $credits")
        return Precondition(context.getString(R.string.precondition_3), context.getString(R.string.precondition_3_desc, credits), credits >= 2200)
    }

    @MainThread
    fun sendDarkThemeTo(username: String?): LiveData<Resource<Boolean>> {
        val result = MutableLiveData<Resource<Boolean>>()
        result.value = Resource.loading(false)

        executors.networkIO().execute {
            try {
                val response = service.requestDarkSendTo(DarkInvite(username)).execute()
                val code = response.code()
                Timber.d("Response code $code")
                if (code == 200) {
                    try {
                        val accResponse = service.getAccount().execute()
                        if (accResponse.isSuccessful) {
                            val item = accResponse.body()
                            if (item != null) {
                                database.accountDao().insert(item)
                            }
                        }
                    } catch (ignored: Throwable) { }
                    result.postValue(Resource.success(true))
                } else {
                    result.postValue(Resource.error("Invalid request", code, Exception("Nothing special")))
                }
            } catch (t: Throwable) {
                result.postValue(Resource.error("Invalid for all", 500, t))
            }
        }
        return result
    }
}
