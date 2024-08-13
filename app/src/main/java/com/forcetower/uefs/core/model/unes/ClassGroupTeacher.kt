package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [
    Index(value = ["classGroupId", "teacherId"], unique = true),
    Index(value = ["teacherId"], unique = false),
    Index(value = ["classGroupId"], unique = false),
], foreignKeys = [
    ForeignKey(entity = ClassGroup::class, parentColumns = ["uid"], childColumns = ["classGroupId"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
    ForeignKey(entity = Teacher::class, parentColumns = ["uid"], childColumns = ["teacherId"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)
])
data class ClassGroupTeacher(
    @PrimaryKey(autoGenerate = true)
    val uid: Int,
    val classGroupId: Long,
    val teacherId: Long
)