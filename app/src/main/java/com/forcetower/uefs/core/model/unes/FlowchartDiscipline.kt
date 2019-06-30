package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(foreignKeys = [
    ForeignKey(entity = Discipline::class, childColumns = ["disciplineId"], parentColumns = ["uid"], onUpdate = CASCADE, onDelete = CASCADE),
    ForeignKey(entity = FlowchartSemester::class, childColumns = ["semesterId"], parentColumns = ["id"], onUpdate = CASCADE, onDelete = CASCADE)
], indices = [
    Index(value = ["disciplineId"]),
    Index(value = ["semesterId"])
])
data class FlowchartDiscipline(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val disciplineId: Long,
    val type: String,
    val mandatory: Boolean,
    val semesterId: Long,
    val completed: Boolean,
    val participating: Boolean
)

data class FlowchartDisciplineUI(
    val id: Long,
    val type: String,
    val mandatory: Boolean,
    val name: String,
    val code: String,
    val credits: Int,
    val department: String,
    val program: String? = null,
    val completed: Boolean,
    val participating: Boolean
)