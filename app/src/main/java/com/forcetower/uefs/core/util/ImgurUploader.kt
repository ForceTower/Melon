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

package com.forcetower.uefs.core.util

import androidx.annotation.WorkerThread
import com.forcetower.uefs.core.model.api.ImgurUpload
import com.forcetower.uefs.core.model.api.UploadResponse
import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

object ImgurUploader {
    @WorkerThread
    fun upload(client: OkHttpClient, base64: String, name: String): ImgurUpload? {
        val body = FormBody.Builder()
            .add("album", "YuKcQI3mEuYFu3q")
            .add("image", base64)
            .add("type", "base64")
            .add("name", name)
            .build()

        val request = Request.Builder()
            .addHeader("Authorization", "Client-ID 5becc567d624bcf")
            .addHeader("Accept", "application/json")
            .url("https://api.imgur.com/3/image")
            .post(body)
            .build()

        val call = client.newCall(request)
        try {
            val response = call.execute()
            return if (response.isSuccessful) {
                Timber.d("Upload Success")
                val string = response.body!!.string()
                val converted = Gson().fromJson(string, UploadResponse::class.java)
                Timber.d("Converted $converted")
                converted.data
            } else {
                Timber.d("Failed to upload with code ${response.code}")
                null
            }
        } catch (e: IOException) {
            Timber.e(e, "Failed to upload with exception")
        }

        return null
    }
}
