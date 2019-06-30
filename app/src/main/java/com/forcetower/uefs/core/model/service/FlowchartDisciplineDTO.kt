package com.forcetower.uefs.core.model.service

import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.FlowchartDiscipline
import com.forcetower.uefs.core.model.unes.FlowchartRequirement
import com.google.gson.annotations.SerializedName

data class FlowchartDisciplineDTO(
    val id: Long,
    val name: String,
    val code: String,
    val credits: Int,
    val department: String,
    @SerializedName("department_code")
    val departmentCode: String,
    val program: String?,
    val type: String,
    val mandatory: Boolean,
    @SerializedName("type_id")
    val typeId: Long,
    val completed: Boolean,
    val participating: Boolean,
    val requirements: List<FlowchartRequirement>
) {
    fun toLocalDiscipline(): Discipline {
        return Discipline(0, name, "$departmentCode$code", credits, department, program)
    }

    fun toDiscipline(localDisciplineId: Long, semesterId: Long): FlowchartDiscipline {
        return FlowchartDiscipline(id, localDisciplineId, type, mandatory, semesterId, completed, participating)
    }
}
