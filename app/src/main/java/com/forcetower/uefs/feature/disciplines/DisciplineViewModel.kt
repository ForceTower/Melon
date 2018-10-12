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
import com.forcetower.uefs.core.storage.database.accessors.GroupWithClass
import com.forcetower.uefs.core.storage.repository.DisciplinesRepository
import com.forcetower.uefs.feature.shared.setValueIfNew
import javax.inject.Inject

class DisciplineViewModel @Inject constructor(
    private val repository: DisciplinesRepository
): ViewModel() {
    val semesters by lazy { repository.getParticipatingSemesters() }
    fun classes(semesterId: Long) = repository.getClassesWithGradesFromSemester(semesterId)

    private val classGroupId = MutableLiveData<Long?>()

    private val _group = MediatorLiveData<GroupWithClass?>()
    val group: LiveData<GroupWithClass?>
        get() = _group

    private val _absences = MediatorLiveData<List<ClassAbsence>>()
    val absences: LiveData<List<ClassAbsence>>
        get() = _absences

    init {
        _group.addSource(classGroupId) {
            refreshGroup(it)
        }
        _absences.addSource(classGroupId) {
            refreshAbsences(it)
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

    private fun refreshGroup(classGroupId: Long?) {
        if (classGroupId != null) {
            val source = repository.getClassGroup(classGroupId)
            _group.addSource(source) { value ->
                _group.value = value
            }
        }
    }

    fun setClassGroupId(classGroupId: Long?) {
        this.classGroupId.setValueIfNew(classGroupId)
    }
}