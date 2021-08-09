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

package com.forcetower.uefs.feature.evaluation.rating

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.core.storage.repository.EvaluationRepository
import com.forcetower.uefs.core.storage.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EvaluationRatingViewModel @Inject constructor(
    private val evaluationRepository: EvaluationRepository
) : ViewModel() {
    private var teacher: Boolean = false
    private var teacherId: Long? = null

    private var code: String? = null
    private var department: String? = null

    private val _nextQuestion = MutableLiveData<Event<Unit>>()
    val nextQuestion: LiveData<Event<Unit>>
        get() = _nextQuestion

    fun getQuestionsForTeacher(teacherId: Long): LiveData<Resource<List<Question>>> {
        return evaluationRepository.getQuestionsForTeacher(teacherId)
    }

    fun getQuestionsForDiscipline(code: String, department: String): LiveData<Resource<List<Question>>> {
        return evaluationRepository.getQuestionsForDiscipline(code, department)
    }

    fun onNextQuestion() {
        _nextQuestion.value = Event(Unit)
    }

    fun initForTeacher(teacherId: Long) {
        this.teacher = true
        this.teacherId = teacherId
    }

    fun initForDiscipline(code: String, department: String) {
        this.teacher = false
        this.code = code
        this.department = department
    }

    fun answer(id: Long, rating: Float) {
        val data = mutableMapOf<String, Any?>()
        data["question_id"] = id
        data["rating"] = rating
        data["teacher"] = teacher
        if (teacher) {
            data["teacher_id"] = teacherId
        } else {
            data["code"] = code
            data["department"] = department
        }
        evaluationRepository.answer(data)
    }
}
