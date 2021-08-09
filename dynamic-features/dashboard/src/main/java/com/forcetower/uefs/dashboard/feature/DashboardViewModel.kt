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

package com.forcetower.uefs.dashboard.feature

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.model.unes.SStudent
import com.forcetower.uefs.core.storage.database.aggregation.ClassLocationWithData
import com.forcetower.uefs.core.storage.repository.SagresDataRepository
import com.forcetower.uefs.dashboard.core.storage.repository.DashboardRepository
import com.forcetower.uefs.feature.shared.TimeLiveData
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val dataRepository: SagresDataRepository
) : ViewModel(), AffinityListener {
    private val timing = TimeLiveData(5_000L) {
        Calendar.getInstance().timeInMillis
    }

    val course: LiveData<String?> by lazy { dataRepository.getCourse() }
    val account: LiveData<Account?> = repository.getAccount()
    val student: LiveData<SStudent> = repository.getStudentMe()
    val lastMessage = repository.getLastMessage()
    val affinity = repository.getAffinityQuestions()

    private val _currentClass = MediatorLiveData<ClassLocationWithData?>()
    val currentClass: LiveData<ClassLocationWithData?>
        get() = _currentClass

    val isCurrentClass = currentClass.map {
        val calendar = Calendar.getInstance()
        val currentTimeInt = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startInt = it?.location?.startsAtInt
        if (startInt == null) false
        else {
            startInt <= currentTimeInt
        }
    }

    private val _profileClick = MutableLiveData<Event<Pair<Long, Long>>>()
    val profileClick: LiveData<Event<Pair<Long, Long>>>
        get() = _profileClick

    private val _onMoveToSchedule = MutableLiveData<Event<Unit>>()
    val onMoveToSchedule: LiveData<Event<Unit>>
        get() = _onMoveToSchedule

    init {
        _currentClass.addSource(timing) { _ ->
            val data = repository.getCurrentClass()
            _currentClass.addSource(data) {
                _currentClass.value = it
                _currentClass.removeSource(data)
            }
        }
    }

    fun onProfilePictureClick() {
        val accountId = account.value?.id ?: return
        val studentId = student.value?.id ?: return

        _profileClick.value = Event(accountId to studentId)
    }

    fun onShowAllClasses() {
        _onMoveToSchedule.value = Event(Unit)
    }

    override fun onSelect(questionId: Long, student: SStudent) {
        Timber.d("Selected $student for $questionId")
        repository.scheduleAnswerAffinity(questionId, student.id)
    }

    override fun onShowMoreOptions(questionId: Long) {
        Timber.d("Showing more options for $questionId")
    }
}
