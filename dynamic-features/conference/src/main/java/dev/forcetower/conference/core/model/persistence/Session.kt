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

package dev.forcetower.conference.core.model.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

@Entity(
    foreignKeys = [
        ForeignKey(entity = ConferenceDay::class, parentColumns = ["id"], childColumns = ["dayId"])
    ]
)
data class Session(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    @ColumnInfo(index = true)
    val title: String,
    @ColumnInfo(index = true)
    val description: String,
    val room: String,
    val photoUrl: String?,
    val type: Int,
    @ColumnInfo(index = true)
    val dayId: String
) {
    val hasPhoto inline get() = !photoUrl.isNullOrBlank()

    @Ignore
    val duration = endTime.toInstant().toEpochMilli() - startTime.toInstant().toEpochMilli()

    fun isOverlapping(session: Session): Boolean {
        return this.startTime < session.endTime && this.endTime > session.startTime
    }
}
