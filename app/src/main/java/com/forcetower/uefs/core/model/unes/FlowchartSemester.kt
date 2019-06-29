package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index

@Entity(foreignKeys = [
    ForeignKey(entity = Flowchart::class, childColumns = ["flowchartId"], parentColumns = ["id"], onDelete = CASCADE, onUpdate = CASCADE)
], indices = [
    Index(value = ["flowchartId"])
])
data class FlowchartSemester(
    val id: Long,
    val flowchartId: Long,
    val order: Int,
    val name: String
)

data class FlowchartSemesterUI(
    val id: Long,
    val flowchartId: Long,
    val order: Int,
    val name: String,
    val hours: Int,
    val disciplines: Int
)