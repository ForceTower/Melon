package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Question(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val question: String,
    val description: String?,
    val teacher: Boolean,
    val discipline: Boolean,
    val last: Boolean = false
)