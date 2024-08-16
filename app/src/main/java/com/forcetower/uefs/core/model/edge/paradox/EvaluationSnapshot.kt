package com.forcetower.uefs.core.model.edge.paradox

import com.google.gson.annotations.SerializedName

data class EvaluationSnapshot(
    @SerializedName("teachers")
    val teachers: List<PublicTeacherSnapshot>,
    @SerializedName("disciplines")
    val disciplines: List<PublicDisciplineSnapshot>
)