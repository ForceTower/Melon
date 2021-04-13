/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package dev.forcetower.disciplines.feature

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.model.ui.disciplines.DisciplinesIndexed
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup
import com.forcetower.uefs.core.storage.repository.DisciplinesRepository
import com.forcetower.uefs.core.storage.repository.SagresGradesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class DisciplinesViewModel @Inject constructor(
    private val repository: DisciplinesRepository,
    private val grades: SagresGradesRepository
) : ViewModel(), DisciplinesSemestersActions {
    private val _refreshing = MediatorLiveData<Boolean>()
    override val refreshing: LiveData<Boolean> = _refreshing

    private lateinit var indexer: DisciplinesIndexed
    val disciplines by lazy {
        repository.getAllDisciplinesData().map {
            indexer = it.indexer
            it
        }.asLiveData(Dispatchers.IO)
    }

    private val _scrollToEvent = MutableLiveData<Event<DisciplineScrollEvent>>()
    val scrollToEvent: LiveData<Event<DisciplineScrollEvent>> = _scrollToEvent

    private val _navigateToDisciplineAction = MutableLiveData<Event<ClassFullWithGroup>>()
    val navigateToDisciplineAction: LiveData<Event<ClassFullWithGroup>>
        get() = _navigateToDisciplineAction

    private val _navigateToGroupAction = MutableLiveData<Event<ClassGroup>>()
    val navigateToGroupAction: LiveData<Event<ClassGroup>>
        get() = _navigateToGroupAction

    override fun onSwipeRefresh() {
        Timber.d("Requested to download all disciplines")
        viewModelScope.launch {
            _refreshing.value = true
            // TODO Decide what to do here...
            delay(1000L)
            _refreshing.value = false
        }
    }

    override fun scrollToStartOfSemester(semester: Semester) {
        val index = indexer.positionForSemester(semester)
        _scrollToEvent.value = Event(DisciplineScrollEvent(index, true))
    }

    override fun downloadDisciplines(semester: Semester) {
        Timber.d("Request to download all disciplines...")
        if (_refreshing.value == null || _refreshing.value == false) {
            Timber.d("Something will actually happen")
            _refreshing.value = true
            val result = grades.getGradesAsync(semester.sagresId, false)
            _refreshing.addSource(result) {
                _refreshing.removeSource(result)
                if (it == SagresGradesRepository.SUCCESS) {
                    Timber.d("Completed!")
                }
                _refreshing.value = false
            }
        }
    }

    override fun loadAllDisciplines(view: View): Boolean {
        Timber.d("This is not completed for this fragment yet")
        return true
    }

    override fun classClicked(clazz: ClassFullWithGroup) {
        _navigateToDisciplineAction.value = Event(clazz)
    }

    override fun groupSelected(clazz: ClassGroup) {
        _navigateToGroupAction.value = Event(clazz)
    }
}
