package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(foreignKeys = [
    ForeignKey(entity = FlowchartDiscipline::class, childColumns = ["disciplineId"], parentColumns = ["id"], onDelete = CASCADE, onUpdate = CASCADE),
    ForeignKey(entity = FlowchartDiscipline::class, childColumns = ["requiredDisciplineId"], parentColumns = ["id"], onDelete = CASCADE, onUpdate = CASCADE)
], indices = [
    Index(value = ["disciplineId"]),
    Index(value = ["requiredDisciplineId"])
])
data class FlowchartRequirement(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val type: String,
    @SerializedName("discipline_id")
    val disciplineId: Long,
    @SerializedName("required_discipline_id")
    val requiredDisciplineId: Long?,
    @SerializedName("course_percentage")
    val coursePercentage: Double?,
    @SerializedName("course_hours")
    val courseHours: Long?,
    @SerializedName("type_id")
    val typeId: Int
)

data class FlowchartRequirementUI(
    val id: Long,
    val type: String,
    val shownName: String?,
    val disciplineId: Long,
    val requiredDisciplineId: Long?,
    val coursePercentage: Long?,
    val courseHours: Long?,
    val typeId: Int,
    val sequence: Int?,
    val semesterName: String?,
    val completed: Boolean
)