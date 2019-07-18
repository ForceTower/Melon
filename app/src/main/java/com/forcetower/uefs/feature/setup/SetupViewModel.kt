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

package com.forcetower.uefs.feature.setup

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.service.SyncFrequency
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.storage.repository.FirebaseAuthRepository
import com.forcetower.uefs.core.storage.repository.ProfileRepository
import com.forcetower.uefs.core.work.image.UploadImageToStorage
import com.forcetower.uefs.core.work.sync.SyncLinkedWorker
import com.forcetower.uefs.core.work.sync.SyncMainWorker
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

class SetupViewModel @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val context: Context,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private var selectImageUri: Uri? = null
    private var course: Course? = null
    private var frequency: SyncFrequency = SyncFrequency()
    var syncFrequencies = listOf(SyncFrequency())

    fun uploadImageToStorage() {
        val uri = selectImageUri
        uri ?: return
        UploadImageToStorage.createWorker(context, uri)
    }

    fun setSelectedImage(uri: Uri) {
        selectImageUri = uri
    }

    fun setSelectedCourse(course: Course) {
        this.course = course
    }

    fun getSelectedCourse() = course

    fun updateCourse(course: Course?, user: FirebaseUser) {
        course ?: return
        firebaseAuthRepository.updateCourse(course, user)
        profileRepository.updateUserCourse(course)
    }

    fun setSelectedFrequency(frequency: SyncFrequency) {
        this.frequency = frequency
    }

    fun getSelectedFrequency(): SyncFrequency {
        return frequency
    }

    fun setFrequencyAndComplete(frequency: SyncFrequency) {
        firebaseAuthRepository.updateFrequency(frequency.value)
        SyncLinkedWorker.stopWorker(context)
        SyncMainWorker.createWorker(context, frequency.value)
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString("stg_sync_frequency", frequency.value.toString())
            .apply()
    }
}