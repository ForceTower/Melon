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

package com.forcetower.uefs.core.model.siecomp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime

@Entity(
    indices = [
        Index(value = ["uuid"], unique = true)
    ]
)
data class Session(
    @SerializedName(value = "uid", alternate = ["id"])
    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0,
    @ColumnInfo(name = "day_id")
    var day: Int = 0,
    @ColumnInfo(name = "start_time")
    @SerializedName("start_time")
    var startTime: ZonedDateTime = ZonedDateTime.now(),
    @ColumnInfo(name = "end_time")
    @SerializedName("end_time")
    var endTime: ZonedDateTime = ZonedDateTime.now(),
    var title: String = "",
    var room: String = "",
    var resume: String = "",
    @SerializedName("photo_url")
    var photoUrl: String = "",
    var uuid: String = "",
    var type: Int = 0
) : Comparable<Session> {

    @Ignore
    val year = startTime.year
    @Ignore
    val duration = endTime.toInstant().toEpochMilli() - startTime.toInstant().toEpochMilli()
    @Ignore
    val sessionType = when (type) {
        0 -> SessionType.SPEAK
        1 -> SessionType.WORKSHOP
        2 -> SessionType.DEBATE
        3 -> SessionType.CONCLUSION
        4 -> SessionType.TALK
        5 -> SessionType.PROJECT
        else -> SessionType.UNKNOWN
    }

    fun isLive(): Boolean {
        val now = ZonedDateTime.now()
        return startTime <= now && endTime >= now
    }

    fun isOverlapping(session: Session): Boolean {
        return this.startTime < session.endTime && this.endTime > session.startTime
    }

    override fun compareTo(other: Session): Int {
        val value = startTime.compareTo(other.startTime)
        return if (value != 0)
            value
        else
            duration.compareTo(other.duration)
    }
}
