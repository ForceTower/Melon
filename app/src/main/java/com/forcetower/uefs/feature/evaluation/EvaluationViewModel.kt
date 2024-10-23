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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.forcetower.core.lifecycle.Event
import com.forcetower.core.lifecycle.viewmodel.StateViewModel
import com.forcetower.uefs.core.model.edge.paradox.EvaluationHotTopic
import com.forcetower.uefs.core.model.edge.paradox.PublicHotEvaluationDiscipline
import com.forcetower.uefs.core.model.edge.paradox.PublicHotEvaluationTeacher
import com.forcetower.uefs.core.model.edge.paradox.PublicTeacherEvaluationCombinedData
import com.forcetower.uefs.core.model.edge.paradox.PublicTeacherEvaluationData
import com.forcetower.uefs.core.model.unes.EdgeParadoxSearchableItem
import com.forcetower.uefs.core.storage.repository.EvaluationRepository
import com.forcetower.uefs.core.storage.repository.cloud.EdgeAccountRepository
import com.forcetower.uefs.core.storage.repository.cloud.EdgeAuthRepository
import com.forcetower.uefs.core.util.TextTransformUtils
import com.forcetower.uefs.domain.model.paradox.DisciplineCombinedData
import com.forcetower.uefs.feature.evaluation.discipline.DisciplineInteractor
import com.forcetower.uefs.feature.evaluation.discipline.TeacherInt
import com.forcetower.uefs.feature.evaluation.home.HomeInteractor
import com.forcetower.uefs.feature.evaluation.search.EntitySelector
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class EvaluationViewModel @Inject constructor(
    private val edgeAuthRepository: EdgeAuthRepository,
    private val edgeAccountRepository: EdgeAccountRepository,
    private val evaluationRepository: EvaluationRepository
) : StateViewModel<EvaluationState>(EvaluationState()), HomeInteractor, EntitySelector, DisciplineInteractor {

    var query = ""
        set(value) {
            field = TextTransformUtils.transform(value)
        }

    val searchSource = evaluationRepository.queryEntities { query }.cachedIn(viewModelScope)

    private val _disciplineSelect = MutableLiveData<Event<PublicHotEvaluationDiscipline>>()
    val disciplineSelect: LiveData<Event<PublicHotEvaluationDiscipline>> = _disciplineSelect

    private val _disciplineSelectTeacher = MutableLiveData<Event<PublicTeacherEvaluationData>>()
    val disciplineSelectTeacher: LiveData<Event<PublicTeacherEvaluationData>> = _disciplineSelectTeacher

    private val _teacherSelect = MutableLiveData<Event<PublicHotEvaluationTeacher>>()
    val teacherSelect: LiveData<Event<PublicHotEvaluationTeacher>> = _teacherSelect

    private val _trending = MutableLiveData<List<EvaluationHotTopic>>()
    val trending: LiveData<List<EvaluationHotTopic>> = _trending

    private val _discipline = MutableLiveData<DisciplineCombinedData>()
    val discipline: LiveData<DisciplineCombinedData> = _discipline

    private val _teacher = MutableLiveData<PublicTeacherEvaluationCombinedData>()
    val teacher: LiveData<PublicTeacherEvaluationCombinedData> = _teacher

    private val _teacherIntSelect = MutableLiveData<Event<TeacherInt>>()
    val teacherIntSelect: LiveData<Event<TeacherInt>>
        get() = _teacherIntSelect

    private val _entitySelected = MutableLiveData<Event<EdgeParadoxSearchableItem>>()
    val entitySelect: LiveData<Event<EdgeParadoxSearchableItem>> = _entitySelected

    fun getToken() = edgeAuthRepository.getAccessToken().asLiveData()
    fun getAccount() = edgeAccountRepository.getAccount().asLiveData()

    fun fetchTrending() {
        viewModelScope.launch {
            setState { it.copy(loading = true) }
            runCatching {
                val data = evaluationRepository.getTrendingList()
                _trending.value = data
            }.onFailure {
                setState { it.copy(failed = true) }
            }
            setState { it.copy(loading = false) }
        }
    }

    override fun onClickDiscipline(discipline: PublicHotEvaluationDiscipline) {
        _disciplineSelect.value = Event(discipline)
    }

    override fun onClickTeacherDiscipline(discipline: PublicTeacherEvaluationData) {
        _disciplineSelectTeacher.value = Event(discipline)
    }

    override fun onClickTeacher(teacher: PublicHotEvaluationTeacher) {
        _teacherSelect.value = Event(teacher)
    }

    fun fetchDiscipline(id: String) {
        viewModelScope.launch {
            setState { it.copy(loading = true) }
            runCatching {
                val data = evaluationRepository.getDiscipline(id)
                _discipline.value = data
            }.onFailure {
                setState { it.copy(failed = true) }
            }
            setState { it.copy(loading = false) }
        }
    }

    fun fetchTeacher(id: String) {
        viewModelScope.launch {
            setState { it.copy(loading = true) }
            runCatching {
                val data = evaluationRepository.getTeacherById(id)
                _teacher.value = data
            }.onFailure {
                setState { it.copy(failed = true) }
            }
            setState { it.copy(loading = false) }
        }
    }

//    fun getTeacher(teacherName: String): LiveData<Resource<EvaluationTeacher>> {
//        if (_teacher == null) {
//            _teacher = evaluationRepository.getTeacherByName(teacherName)
//        }
//        return _teacher!!
//    }

    override fun onTeacherSelected(value: TeacherInt) {
        _teacherIntSelect.value = Event(value)
    }

    override fun onEntitySelected(entity: EdgeParadoxSearchableItem?) {
        entity ?: return
        _entitySelected.value = Event(entity)
    }

    fun downloadDatabase() {
        setState { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching {
                evaluationRepository.downloadKnowledgeDatabase()
            }.onFailure {
                setState { it.copy(failed = true) }
            }
            setState { it.copy(loading = false) }
        }
    }
}
