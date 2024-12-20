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

package com.forcetower.uefs.feature.setup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.forcetower.uefs.core.model.service.SyncFrequency
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.storage.repository.ProfileRepository
import com.forcetower.uefs.core.work.image.UploadImageToStorage
import com.forcetower.uefs.core.work.sync.SyncLinkedWorker
import com.forcetower.uefs.core.work.sync.SyncMainWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    application: Application,
    private val profileRepository: ProfileRepository
) : AndroidViewModel(application) {
    private var selectImageUri: Uri? = null
    private var course: Course? = null
    private var frequency: SyncFrequency = SyncFrequency()
    var syncFrequencies = listOf(SyncFrequency())

    fun uploadImageToStorage() {
        val uri = selectImageUri
        uri ?: return
        UploadImageToStorage.createWorker(getApplication(), uri)
    }

    fun setSelectedImage(uri: Uri) {
        selectImageUri = uri
    }

    fun setSelectedCourse(course: Course) {
        this.course = course
    }

    fun getSelectedCourse() = course

    fun updateCourse(course: Course?) {
        course ?: return
        profileRepository.updateUserCourse(course)
    }

    fun setSelectedFrequency(frequency: SyncFrequency) {
        this.frequency = frequency
    }

    fun getSelectedFrequency(): SyncFrequency {
        return frequency
    }

    fun setFrequencyAndComplete(frequency: SyncFrequency) {
        SyncLinkedWorker.stopWorker(getApplication())
        SyncMainWorker.createWorker(getApplication(), frequency.value)
        PreferenceManager.getDefaultSharedPreferences(getApplication()).edit()
            .putString("stg_sync_frequency", frequency.value.toString())
            .apply()
    }
}
