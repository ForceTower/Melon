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

package com.forcetower.uefs.feature.setup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.R
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
    private val preferences: SharedPreferences,
    private val context: Context,
    private val profileRepository: ProfileRepository
): ViewModel() {
    private var selectImageUri: Uri? = null
    private var course: Course? = null
    private var frequency: Frequency = Frequency(context.getString(R.string.at_every_time_hour), 60)

    fun uploadImageToStorage(reference: String) {
        val uri = selectImageUri
        uri?: return
        UploadImageToStorage.createWorker(uri, reference)
    }

    fun setSelectedImage(uri: Uri) {
        selectImageUri = uri
    }

    fun setSelectedCourse(course: Course) {
        this.course = course
    }

    fun getSelectedCourse() = course

    fun updateCourse(course: Course, user: FirebaseUser) {
        firebaseAuthRepository.updateCourse(course, user)
        profileRepository.updateUserCourse(course)
    }

    fun setSelectedFrequency(frequency: Frequency) {
        this.frequency = frequency
    }

    fun getSelectedFrequency(): Frequency {
        return frequency
    }

    fun setFrequencyAndComplete(frequency: Frequency) {
        firebaseAuthRepository.updateFrequency(frequency.value)
        SyncLinkedWorker.stopWorker()
        SyncMainWorker.createWorker(context, frequency.value)
    }
}