package com.forcetower.uefs.feature.evaluation.home

import com.forcetower.uefs.core.model.service.EvaluationDiscipline
import com.forcetower.uefs.core.model.service.EvaluationTeacher

interface HomeInteractor {
    fun onClickDiscipline(discipline: EvaluationDiscipline)
    fun onClickTeacher(teacher: EvaluationTeacher)
}