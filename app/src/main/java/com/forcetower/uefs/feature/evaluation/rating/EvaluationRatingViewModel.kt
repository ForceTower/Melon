package com.forcetower.uefs.feature.evaluation.rating

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.core.storage.repository.EvaluationRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.vm.Event
import javax.inject.Inject

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
        data["value"] = rating
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