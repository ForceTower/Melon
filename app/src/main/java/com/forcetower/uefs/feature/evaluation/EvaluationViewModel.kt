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

package com.forcetower.uefs.feature.evaluation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.model.service.EvaluationDiscipline
import com.forcetower.uefs.core.model.service.EvaluationHomeTopic
import com.forcetower.uefs.core.model.service.EvaluationTeacher
import com.forcetower.uefs.core.model.unes.EvaluationEntity
import com.forcetower.uefs.core.storage.repository.AccountRepository
import com.forcetower.uefs.core.storage.repository.EvaluationRepository
import com.forcetower.uefs.core.storage.repository.cloud.AuthRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.util.TextTransformUtils
import com.forcetower.uefs.feature.evaluation.discipline.DisciplineInteractor
import com.forcetower.uefs.feature.evaluation.discipline.TeacherInt
import com.forcetower.uefs.feature.evaluation.home.HomeInteractor
import com.forcetower.uefs.feature.evaluation.search.EntitySelector
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EvaluationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val evaluationRepository: EvaluationRepository
) : ViewModel(), HomeInteractor, EntitySelector, DisciplineInteractor {
    private var _trending: LiveData<Resource<List<EvaluationHomeTopic>>>? = null

    var query = ""
        set(value) {
            field = TextTransformUtils.transform(value)
        }

    val searchSource = evaluationRepository.queryEntities { query }.cachedIn(viewModelScope)

    private val _disciplineSelect = MutableLiveData<Event<EvaluationDiscipline>>()

    val disciplineSelect: LiveData<Event<EvaluationDiscipline>>
        get() = _disciplineSelect
    private val _teacherSelect = MutableLiveData<Event<EvaluationTeacher>>()

    val teacherSelect: LiveData<Event<EvaluationTeacher>>
        get() = _teacherSelect
    private var _discipline: LiveData<Resource<EvaluationDiscipline>>? = null
    private var _teacher: LiveData<Resource<EvaluationTeacher>>? = null

    private var _knowledge: LiveData<Resource<Boolean>>? = null

    private val _teacherIntSelect = MutableLiveData<Event<TeacherInt>>()
    val teacherIntSelect: LiveData<Event<TeacherInt>>
        get() = _teacherIntSelect

    private val _entitySelected = MutableLiveData<Event<EvaluationEntity>>()
    val entitySelect: LiveData<Event<EvaluationEntity>>
        get() = _entitySelected

    fun getToken() = authRepository.getAccessToken()
    fun getAccount() = accountRepository.getAccount()
    fun getTrendingList(): LiveData<Resource<List<EvaluationHomeTopic>>> {
        if (_trending == null) {
            _trending = evaluationRepository.getTrendingList()
        }
        return _trending!!
    }

    override fun onClickDiscipline(discipline: EvaluationDiscipline) {
        _disciplineSelect.value = Event(discipline)
    }

    override fun onClickTeacher(teacher: EvaluationTeacher) {
        _teacherSelect.value = Event(teacher)
    }

    fun getDiscipline(department: String, code: String): LiveData<Resource<EvaluationDiscipline>> {
        if (_discipline == null) {
            _discipline = evaluationRepository.getDiscipline(department, code)
        }
        return _discipline!!
    }

    fun getTeacher(teacherId: Long): LiveData<Resource<EvaluationTeacher>> {
        if (_teacher == null) {
            _teacher = evaluationRepository.getTeacherById(teacherId)
        }
        return _teacher!!
    }

    fun getTeacher(teacherName: String): LiveData<Resource<EvaluationTeacher>> {
        if (_teacher == null) {
            _teacher = evaluationRepository.getTeacherByName(teacherName)
        }
        return _teacher!!
    }

    override fun onTeacherSelected(value: TeacherInt) {
        _teacherIntSelect.value = Event(value)
    }

    override fun onEntitySelected(entity: EvaluationEntity) {
        _entitySelected.value = Event(entity)
    }

    fun downloadDatabase(): LiveData<Resource<Boolean>> {
        if (_knowledge == null) {
            _knowledge = evaluationRepository.downloadKnowledgeDatabase()
        }
        return _knowledge!!
    }
}
