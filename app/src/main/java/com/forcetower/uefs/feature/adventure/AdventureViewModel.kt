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

package com.forcetower.uefs.feature.adventure

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.model.service.AchDistance
import com.forcetower.uefs.core.model.service.Achievement
import com.forcetower.uefs.core.storage.repository.AdventureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
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

    fun checkAchievements(): LiveData<Map<Int, Int>> {
        return repository.checkAchievements()
    }

    fun checkServerAchievements(): LiveData<List<Achievement>> {
        return repository.checkServerAchievements()
    }

    fun checkNotConnectedAchievements(): LiveData<Map<Int, Int>> {
        return repository.justCheckAchievements()
    }

    fun onReceiveLocation(location: Location?): List<AchDistance> {
        return repository.matchesAnyAchievement(location)
    }
}
