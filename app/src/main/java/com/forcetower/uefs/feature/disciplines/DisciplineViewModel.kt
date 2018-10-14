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

package com.forcetower.uefs.feature.disciplines

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.model.unes.ClassItem
import com.forcetower.uefs.core.model.unes.ClassMaterial
import com.forcetower.uefs.core.storage.database.accessors.ClassStudentWithGroup
import com.forcetower.uefs.core.storage.database.accessors.ClassWithGroups
import com.forcetower.uefs.core.storage.database.accessors.GroupWithClass
import com.forcetower.uefs.core.storage.repository.DisciplinesRepository
import com.forcetower.uefs.core.vm.Event
import com.forcetower.uefs.feature.common.DisciplineActions
import com.forcetower.uefs.feature.shared.map
import com.forcetower.uefs.feature.shared.setValueIfNew
import javax.inject.Inject

class DisciplineViewModel @Inject constructor(
    private val repository: DisciplinesRepository
): ViewModel(), DisciplineActions {

    val semesters by lazy { repository.getParticipatingSemesters() }
    fun classes(semesterId: Long) = repository.getClassesWithGradesFromSemester(semesterId)

    private val classGroupId = MutableLiveData<Long?>()

    private val _classStudent = MediatorLiveData<ClassStudentWithGroup?>()
    val classStudent: LiveData<ClassStudentWithGroup?>
        get() = _classStudent

    val group: LiveData<GroupWithClass?> = classStudent.map {
        it?.group()
    }

    private val _absences = MediatorLiveData<List<ClassAbsence>>()
    val absences: LiveData<List<ClassAbsence>>
        get() = _absences

    private val _materials = MediatorLiveData<List<ClassMaterial>>()
    val materials: LiveData<List<ClassMaterial>>
        get() = _materials

    private val _classItems = MediatorLiveData<List<ClassItem>>()
    val classItems: LiveData<List<ClassItem>>
        get() = _classItems

    private val _loadClassDetails = MediatorLiveData<Long>()
    val loadClassDetails: LiveData<Long>
        get() = _loadClassDetails

    private val _navigateToDisciplineAction = MutableLiveData<Event<ClassWithGroups>>()
    val navigateToDisciplineAction: LiveData<Event<ClassWithGroups>>
        get() = _navigateToDisciplineAction

    init {
        _classStudent.addSource(classGroupId) {
            refreshClassStudent(it)
        }
        _absences.addSource(classGroupId) {
            refreshAbsences(it)
        }
        _materials.addSource(classGroupId) {
            refreshMaterials(it)
        }
        _classItems.addSource(classGroupId) {
            refreshClassItems(it)
        }
        _loadClassDetails.addSource(classGroupId) {
            if (it != null) repository.loadClassDetails(it)
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

    private fun refreshAbsences(classGroupId: Long?) {
        if (classGroupId != null) {
            val source = repository.getMyAbsencesFromGroup(classGroupId)
            _absences.addSource(source) { value ->
                _absences.value = value
            }
        }
    }

    private fun refreshClassStudent(classGroupId: Long?) {
        if (classGroupId != null) {
            val source = repository.getClassStudent(classGroupId)
            _classStudent.addSource(source) { value ->
                _classStudent.value = value
            }
        }
    }

    fun setClassGroupId(classGroupId: Long?) {
        this.classGroupId.setValueIfNew(classGroupId)
    }

    override fun classClicked(clazz: ClassWithGroups) {
        _navigateToDisciplineAction.value = Event(clazz)
    }
}