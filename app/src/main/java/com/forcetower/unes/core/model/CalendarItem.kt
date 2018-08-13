package com.forcetower.unes.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.SagresCalendar
import java.util.*

@Entity
data class CalendarItem(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val message: String,
    val date: String,
    val uuid: String = UUID.randomUUID().toString()
) {
    companion object {
        fun fromSagres(item: SagresCalendar) = CalendarItem(message = item.message, date = item.day)
    }
}