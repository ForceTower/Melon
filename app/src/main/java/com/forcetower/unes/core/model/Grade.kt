package com.forcetower.unes.core.model

import androidx.room.*
import androidx.room.ForeignKey.CASCADE

@Entity(foreignKeys = [
    ForeignKey(entity = ClassStudent::class, parentColumns = ["uid"], childColumns = ["class_id"], onUpdate = CASCADE, onDelete = CASCADE)
], indices = [
    Index(value = ["name", "class_id"])
])
data class Grade(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val name: String,
    val date: String,
    val notified: Int = 0,
    @ColumnInfo(name = "class_id")
    val classId: Long
)