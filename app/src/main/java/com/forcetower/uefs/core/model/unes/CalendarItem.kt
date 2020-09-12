/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.SagresCalendar
import java.util.UUID

@Entity(
    indices = [
        Index(value = ["uuid"], unique = true)
    ]
)
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
