package com.forcetower.unes.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.Semester

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
) {
    companion object {
        fun fromSagres(s: Semester) =
                Semester(0, s.uefsId, s.name.trim(), s.codename.trim(), s.startInMillis, s.endInMillis, s.startClassesInMillis, s.endClassesInMillis)
    }
}