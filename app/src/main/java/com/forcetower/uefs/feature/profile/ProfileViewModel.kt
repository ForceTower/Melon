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

package com.forcetower.uefs.feature.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.storage.repository.ProfileRepository
import com.forcetower.uefs.feature.shared.setValueIfNew
import timber.log.Timber
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
    val repository: ProfileRepository
) : ViewModel() {
    private val profileId = MutableLiveData<String?>()

    private val _profile = MediatorLiveData<Profile?>()
    val profile: LiveData<Profile?>
        get() = _profile

    private val _course = MediatorLiveData<Course>()
    val course: LiveData<Course>
        get() = _course

    private val _disciplineCount = MutableLiveData<Int>()
    val disciplineCount: LiveData<Int>
        get() = _disciplineCount

    private val _hoursCount = MutableLiveData<Int>()
    val hoursCount: LiveData<Int>
        get() = _hoursCount

    init {
        _profile.addSource(profileId) {
            refreshProfile(it)
        }
    }

    private fun refreshProfile(profileId: String?) {
        if (profileId != null) {
            val source = repository.loadProfile(profileId)
            _profile.addSource(source) {
                _profile.value = it

                if (it != null) {
                    updateCourse(it)
                    updateDisciplines(it)
                }
            }
        } else {
            Timber.d("No profile information available")
        }
    }

    private fun updateCourse(profile: Profile) {
        val course = profile.course
        if (course != null) {
            val source = repository.getCourse(profile.course)
            _course.addSource(source) {
                _course.value = it
            }
        }
    }

    private fun updateDisciplines(@Suppress("UNUSED_PARAMETER") profile: Profile) {
        val source = repository.getProfileClasses()
        _profile.addSource(source) { values ->
            Timber.d("Discipline List")
            val hours = values.asSequence().map { it.singleDiscipline().credits }.sum()
            val size = values.size

            _hoursCount.value = hours
            _disciplineCount.value = size
        }
    }

    fun getMeProfile() = repository.getMeProfile()

    fun setProfileId(newProfileId: String?) {
        profileId.setValueIfNew(newProfileId)
    }
}