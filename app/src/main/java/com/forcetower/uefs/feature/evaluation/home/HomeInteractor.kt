package com.forcetower.uefs.feature.evaluation.home

import com.forcetower.uefs.core.model.service.EvaluationDiscipline

interface HomeInteractor {
    fun onClickDiscipline(discipline: EvaluationDiscipline)
}