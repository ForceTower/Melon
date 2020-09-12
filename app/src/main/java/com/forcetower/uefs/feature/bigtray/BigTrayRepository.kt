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

package com.forcetower.uefs.feature.bigtray

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.bigtray.BigTrayData
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject

class BigTrayRepository @Inject constructor(
    private val client: OkHttpClient,
    private val executors: AppExecutors
) {
    var requesting = false

    private var _data = MutableLiveData<BigTrayData>()
    val data: LiveData<BigTrayData>
        get() {
            beginRequests()
            return _data
        }

    fun beginWith(delay: Long = 3500): LiveData<BigTrayData> {
        requesting = true
        loop(delay)
        return _data
    }

    private fun beginRequests(delay: Long = 3500) {
        requesting = true
        loop(delay)
    }

    private fun loop(delay: Long) {
        val handler = Handler(Looper.getMainLooper())
        if (requesting) {
            executors.networkIO().execute {
                val data = performRequest()
                _data.postValue(data)
                handler.postDelayed({ this.loop(delay) }, delay)
            }
        }
    }

    private fun performRequest(): BigTrayData {
        val request = createRequest()
        val call = client.newCall(request)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val value = response.body!!.string()
                if (value.equals("false", ignoreCase = true)) {
                    return BigTrayData.closed()
                } else {
                    val values = value.split(";")
                    if (values.size == 2) {
                        return BigTrayData.createData(values)
                    } else {
                        Timber.e("The size of the big tray has changed to ${values.size}")
                    }
                }
            }
        } catch (ignored: Throwable) {}

        return BigTrayData.error()
    }

    private fun createRequest() =
        Request.Builder()
            .url("http://www.propaae.uefs.br/ru/getCotas.php")
            .get()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.92 Safari/537.36")
            .build()
}
