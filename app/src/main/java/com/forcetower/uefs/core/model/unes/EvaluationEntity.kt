package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [
    Index(value = ["name"])
])
data class EvaluationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val referencedId: Long,
    val name: String,
    val extra: String?,
    val image: String?,
    val type: Int,
    val searchable: String,
    val comp1: String? = null,
    val comp2: String? = null
)