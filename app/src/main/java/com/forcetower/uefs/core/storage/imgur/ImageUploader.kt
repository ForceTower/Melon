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

package com.forcetower.uefs.core.storage.imgur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.forcetower.uefs.core.model.api.ImgurUpload
import com.forcetower.uefs.core.util.ImgurUploader
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

object ImageUploader {
    fun uploadToImGur(
        uri: Uri,
        context: Context,
        client: OkHttpClient,
        name: String = UUID.randomUUID().toString()
    ): ImgurUpload? {
        val resolver = context.applicationContext.contentResolver
        val stream: InputStream
        try {
            stream = resolver.openInputStream(uri) ?: throw Exception("Failed to load stream")
        } catch (exception: Throwable) {
            Timber.e(exception, "Error uploading image...")
            return null
        }

        val bitmap = BitmapFactory.decodeStream(stream)

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()
        val encoded = Base64.encodeToString(data, Base64.DEFAULT)
        return ImgurUploader.upload(client, encoded, name)
    }
}
