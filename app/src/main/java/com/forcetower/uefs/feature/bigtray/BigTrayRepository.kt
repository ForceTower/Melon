/*
 * Copyright (c) 2019.
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

package com.forcetower.uefs.feature.bigtray

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.crashlytics.android.Crashlytics
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.bigtray.BigTrayData
import okhttp3.OkHttpClient
import okhttp3.Request
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
                val value = response.body()!!.string()
                if (value.equals("false", ignoreCase = true)) {
                    return BigTrayData.closed()
                } else {
                    val values = value.split(";")
                    if (values.size == 2) {
                        return BigTrayData.createData(values)
                    } else {
                        Crashlytics.log("The size of the big tray has changed to ${values.size}")
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