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

package com.forcetower.uefs.feature.adventure

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.storage.repository.AdventureRepository
import com.forcetower.uefs.core.vm.Event
import javax.inject.Inject

class AdventureViewModel @Inject constructor(
    private val repository: AdventureRepository
) : ViewModel(), AdventureInteractor {
    private val _locations = MutableLiveData<Boolean>()
    val locations: LiveData<Boolean>
        get() = _locations

    private val _achievements = MutableLiveData<Event<Any?>>()
    val achievements: LiveData<Event<Any?>>
        get() = _achievements

    private val _leave = MutableLiveData<Event<Boolean>>()
    val leave: LiveData<Event<Boolean>>
        get() = _leave

    private val _start = MutableLiveData<Event<Any?>>()
    val start: LiveData<Event<Any?>>
        get() = _start

    override fun beginAdventure() {
        _start.value = Event(Any())
    }

    override fun leave() {
        _leave.value = Event(true)
    }

    override fun turnOnLocations() {
        val requesting = _locations.value ?: false
        _locations.value = !requesting
    }

    override fun openAchievements() {
        _achievements.value = Event(Any())
    }

    override fun isConnected(): Boolean {
        return true
    }

    fun checkAchievements(email: String? = null): LiveData<Map<Int, Int>> {
        return repository.checkAchievements(email)
    }

    fun checkNotConnectedAchievements(): LiveData<Map<Int, Int>> {
        return repository.justCheckAchievements()
    }

    fun onReceiveLocation(location: Location): Int? {
        return repository.matchesAnyAchievement(location)
    }
}