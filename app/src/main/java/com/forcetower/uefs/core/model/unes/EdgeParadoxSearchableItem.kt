package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["serviceId", "type"])
    ]
)
data class EdgeParadoxSearchableItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serviceId: String,
    val displayName: String,
    val subtitle: String?,
    val displayImage: String?,
    val type: Int,
    val searchable: String?,
    val optionalReference: String?
) {
    companion object {
        const val TEACHER_TYPE = 0
        const val DISCIPLINE_TYPE = 1
    }
}
