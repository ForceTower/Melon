package com.forcetower.uefs.core.model.service

data class FlowchartDisciplineDTO(
    val id: Long,
    val name: String,
    val code: String,
    val credits: Int,
    val department: String,
    val program: String,
    val type: String,
    val mandatory: Boolean
)
