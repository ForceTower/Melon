package com.forcetower.unes.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["sagres_id"], unique = true)])
data class Semester(
    @PrimaryKey(autoGenerate = true)
    val uid: Long,
    @ColumnInfo(name = "sagres_id")
    val sagresId: Long,
    val name: String,
    val codename: String,
    val start: Long?,
    val end: Long?,
    @ColumnInfo(name = "start_class")
    val startClass: Long?,
    @ColumnInfo(name = "end_class")
    val endClass: Long?
)