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

package com.forcetower.uefs.feature.schedule

import android.view.View
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.forcetower.uefs.core.storage.database.aggregation.ClassGroupWithData
import com.forcetower.uefs.core.storage.database.aggregation.ClassLocationWithData
import com.forcetower.uefs.core.storage.repository.SagresSyncRepository
import com.forcetower.uefs.core.storage.repository.ScheduleRepository
import com.forcetower.uefs.core.storage.repository.SnowpiercerSyncRepository
import com.forcetower.uefs.core.vm.Event
import com.forcetower.uefs.easter.twofoureight.Game2048Activity
import com.forcetower.uefs.feature.disciplines.disciplinedetail.DisciplineDetailsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleViewModel @ViewModelInject constructor(
    repository: ScheduleRepository,
    private val sagresSyncRepository: SagresSyncRepository,
    private val snowpiercerSyncRepository: SnowpiercerSyncRepository
) : ViewModel(), ScheduleActions {

    val hasSchedule = repository.hasSchedule()
    val schedule = repository.getProcessedSchedule().asLiveData(Dispatchers.IO)

    private val _onRefresh = MutableLiveData<Event<Unit>>()
    val onRefresh: LiveData<Event<Unit>> = _onRefresh

    override fun onLongClick(view: View): Boolean {
        Game2048Activity.startActivity(view.context)
        return false
    }

    override fun onClick(view: View, group: ClassGroupWithData) {
        val context = view.context
        val intent = DisciplineDetailsActivity.startIntent(context, group.classData.clazz.uid, group.group.uid)
        context.startActivity(intent)
    }

    override fun onLocationClick(view: View, location: ClassLocationWithData) {
        val group = location.groupData
        if (group.classData.clazz.scheduleOnly) return
        val context = view.context
        val intent = DisciplineDetailsActivity.startIntent(context, group.classData.clazz.uid, group.group.uid)
        context.startActivity(intent)
    }

    fun doRefreshData(gToken: String?, snowpiercer: Boolean) {
        if (snowpiercer) {
            viewModelScope.launch {
                snowpiercerSyncRepository.asyncSync()
            }
        } else {
            sagresSyncRepository.asyncSync(gToken)
        }
    }

    override fun refreshData() {
        _onRefresh.value = Event(Unit)
    }
}
