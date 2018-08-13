package com.forcetower.unes.core.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(indices = [
    Index(value = ["code"], unique = true),
    Index(value = ["uuid"], unique = true),
    Index(value = ["name"], unique = true)
])
data class Discipline(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val name: String,
    val code: String,
    val credits: Int,
    val department: String? = null,
    val uuid: String = UUID.randomUUID().toString()
) {

    companion object {
        fun fromSagres(discipline: com.forcetower.sagres.database.model.Discipline)
                = Discipline(name = discipline.name, code = discipline.code, credits = discipline.credits)
    }
}