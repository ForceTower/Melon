/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.feature.disciplines

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.architecture.service.discipline.DisciplineDetailsLoaderService
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.ClassItem
import com.forcetower.uefs.core.model.unes.ClassMaterial
import com.forcetower.uefs.core.storage.database.accessors.ClassFullWithGroup
import com.forcetower.uefs.core.storage.database.accessors.ClassWithGroups
import com.forcetower.uefs.core.storage.repository.DisciplinesRepository
import com.forcetower.uefs.core.storage.repository.SagresGradesRepository
import com.forcetower.uefs.core.vm.Event
import com.forcetower.uefs.feature.common.DisciplineActions
import com.forcetower.uefs.feature.shared.extensions.setValueIfNew
import timber.log.Timber
import javax.inject.Inject

class DisciplineViewModel @Inject constructor(
    private val repository: DisciplinesRepository,
    private val grades: SagresGradesRepository
) : ViewModel(), DisciplineActions, MaterialActions {
    val semesters by lazy { repository.getParticipatingSemesters() }
    fun classes(semesterId: Long) = repository.getClassesWithGradesFromSemester(semesterId)

    private val classGroupId = MutableLiveData<Long?>()
    private val classId = MutableLiveData<Long?>()

    private val _classFull = MediatorLiveData<ClassFullWithGroup?>()
    val clazz: LiveData<ClassFullWithGroup?>
        get() = _classFull

    private val _group = MediatorLiveData<ClassGroup?>()
    val group: LiveData<ClassGroup?>
        get() = _group

    private val _absences = MediatorLiveData<List<ClassAbsence>>()
    val absences: LiveData<List<ClassAbsence>>
        get() = _absences

    private val _absencesAmount = MediatorLiveData<Int>()
    val absencesAmount: LiveData<Int>
        get() = _absencesAmount

    private val _materials = MediatorLiveData<List<ClassMaterial>>()
    val materials: LiveData<List<ClassMaterial>>
        get() = _materials

    private val _classItems = MediatorLiveData<List<ClassItem>>()
    val classItems: LiveData<List<ClassItem>>
        get() = _classItems

    private val _loadClassDetails = MediatorLiveData<Boolean>()
    val loadClassDetails: LiveData<Boolean>
        get() = _loadClassDetails

    private val _navigateToDisciplineAction = MutableLiveData<Event<ClassWithGroups>>()
    val navigateToDisciplineAction: LiveData<Event<ClassWithGroups>>
        get() = _navigateToDisciplineAction

    private val _navigateToGroupAction = MutableLiveData<Event<ClassGroup>>()
    val navigateToGroupAction: LiveData<Event<ClassGroup>>
        get() = _navigateToGroupAction

    private val _navigateToTeacherAction = MutableLiveData<Event<String>>()
    val navigateToTeacherAction: LiveData<Event<String>>
        get() = _navigateToTeacherAction

    private val _refreshing = MediatorLiveData<Boolean>()
    val refreshing: LiveData<Boolean>
        get() = _refreshing

    private val _materialClick = MutableLiveData<Event<ClassMaterial>>()
    val materialClick: LiveData<Event<ClassMaterial>>
        get() = _materialClick

    init {
        _classFull.addSource(classId) {
            refreshClassStudent(it)
        }
        _absences.addSource(classId) {
            refreshAbsences(it)
        }
        _absencesAmount.addSource(classId) {
            refreshAbsencesAmount(it)
        }
        _materials.addSource(classGroupId) {
            refreshMaterials(it)
        }
        _classItems.addSource(classGroupId) {
            refreshClassItems(it)
        }
        _loadClassDetails.addSource(classGroupId) {
            if (it != null) {
                val src = repository.loadClassDetails(it)
                _loadClassDetails.addSource(src) { loading ->
                    _loadClassDetails.value = loading
                }
            }
        }
        _group.addSource(classGroupId) {
            if (it != null) {
                val source = repository.getClassGroup(it)
                _group.addSource(source) { value ->
                    _group.value = value?.group
                }
            }
        }
    }

    private fun refreshClassItems(classGroupId: Long?) {
        if (classGroupId != null) {
            val source = repository.getClassItemsFromGroup(classGroupId)
            _classItems.addSource(source) { value ->
                _classItems.value = value
            }
        }
    }

    private fun refreshMaterials(classGroupId: Long?) {
        if (classGroupId != null) {
            val source = repository.getMaterialsFromGroup(classGroupId)
            _materials.addSource(source) { value ->
                _materials.value = value
            }
        }
    }

    private fun refreshAbsences(classId: Long?) {
        if (classId != null) {
            val source = repository.getMyAbsencesFromClass(classId)
            _absences.addSource(source) { value ->
                _absences.value = value
            }
        }
    }

    private fun refreshAbsencesAmount(classId: Long?) {
        if (classId != null) {
            val source = repository.getAbsencesAmount(classId)
            _absencesAmount.addSource(source) { value ->
                _absencesAmount.value = value
            }
        }
    }

    private fun refreshClassStudent(classId: Long?) {
        if (classId != null) {
            val source = repository.getClassFull(classId)
            _classFull.addSource(source) { value ->
                _classFull.value = value
            }
        }
    }

    fun setClassGroupId(classGroupId: Long?) {
        this.classGroupId.setValueIfNew(classGroupId)
    }

    fun setClassId(classId: Long?) {
        this.classId.setValueIfNew(classId)
    }

    override fun classClicked(clazz: ClassWithGroups) {
        _navigateToDisciplineAction.value = Event(clazz)
    }

    override fun groupSelected(clazz: ClassGroup) {
        _navigateToGroupAction.value = Event(clazz)
    }

    fun onTeacherNameClick(name: String) {
        Timber.d("Name clicked $name")
        _navigateToTeacherAction.value = Event(name)
    }

    fun updateGradesFromSemester(semesterId: Long) {
        Timber.d("Started refresh")
        if (_refreshing.value == null || _refreshing.value == false) {
            Timber.d("Something will actually happen")
            _refreshing.value = true
            val result = grades.getGradesAsync(semesterId, true)
            _refreshing.addSource(result) {
                _refreshing.removeSource(result)
                if (it == SagresGradesRepository.SUCCESS) {
                    Timber.d("Completed!")
                }
                _refreshing.value = false
            }
        }
    }

    fun resetGroups(clazz: Class?): Boolean {
        clazz ?: return true
        repository.resetGroups(clazz)
        return true
    }

    fun loadAllDisciplines(view: View): Boolean {
        val ctx = view.context
        DisciplineDetailsLoaderService.startService(ctx, true)
        return true
    }

    override fun onMaterialClick(material: ClassMaterial?) {
        material ?: return
        _materialClick.value = Event(material)
    }
}