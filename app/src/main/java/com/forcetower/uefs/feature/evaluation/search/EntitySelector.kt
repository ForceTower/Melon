package com.forcetower.uefs.feature.evaluation.search

import com.forcetower.uefs.core.model.unes.EvaluationEntity

interface EntitySelector {
    fun onEntitySelected(entity: EvaluationEntity)
}