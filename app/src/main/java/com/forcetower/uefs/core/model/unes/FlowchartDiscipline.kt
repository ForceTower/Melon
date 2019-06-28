package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index

@Entity(foreignKeys = [
    ForeignKey(entity = Discipline::class, childColumns = ["disciplineId"], parentColumns = ["uid"], onUpdate = CASCADE, onDelete = CASCADE)
], indices = [
    Index(value = ["disciplineId"])
])
data class FlowchartDiscipline(
    val id: Long,
    val disciplineId: Long,
    val type: String,
    val mandatory: Boolean
)