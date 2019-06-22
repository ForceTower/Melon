package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(indices = [
    Index(value = ["code", "department"], unique = true),
    Index(value = ["name"]),
    Index(value = ["code"]),
    Index(value = ["department"])
])
data class SDiscipline(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("discipline_id")
    val disciplineId: Long,
    val department: String,
    @SerializedName("department_name")
    val departmentName: String?,
    val code: String,
    val name: String
)