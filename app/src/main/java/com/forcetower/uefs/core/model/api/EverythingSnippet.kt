package com.forcetower.uefs.core.model.api

import com.forcetower.uefs.core.model.unes.SDiscipline
import com.forcetower.uefs.core.model.unes.SStudent
import com.forcetower.uefs.core.model.unes.STeacher

data class EverythingSnippet(
    val teachers: List<STeacher>,
    val disciplines: List<SDiscipline>,
    val students: List<SStudent>
)