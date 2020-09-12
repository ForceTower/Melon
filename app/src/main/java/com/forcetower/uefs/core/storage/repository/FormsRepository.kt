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
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.database.UDatabase
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject

class FormsRepository @Inject constructor(
    private val client: OkHttpClient,
    private val executors: AppExecutors,
    private val preferences: SharedPreferences,
    database: UDatabase
) {
    val account = database.accountDao().getAccount()

    fun submitAnswers(data: Map<String, String>) {
        executors.networkIO().execute {
            submitAnswersSync(data)
        }
    }

    private fun submitAnswersSync(data: Map<String, String>) {
        val form = FormBody.Builder().apply {
            data.entries.forEach { this.addEncoded(it.key, it.value) }
            this.addEncoded("fvv", "1")
            this.addEncoded("pageHistory", "0")
        }.build()

        val request = Request.Builder()
            .url("https://docs.google.com/forms/u/0/d/e/1FAIpQLSdSAI5rfAeN7dBf1OGeTl_otPyWmguZg1-_HnazSgqsBxc_Yg/formResponse")
            .post(form)
            .build()

        val call = client.newCall(request)

        try {
            val response = call.execute()
            if (response.isSuccessful) {
                Timber.d("Answers submitted!")
                preferences.edit().putBoolean("answered_forms_satisfaction_pos", true).apply()
            } else {
                Timber.e("Failed to answer survey due to error ${response.code}")
            }
        } catch (error: Throwable) {
            Timber.e(error, "Failed to perform call")
        }
    }
}
