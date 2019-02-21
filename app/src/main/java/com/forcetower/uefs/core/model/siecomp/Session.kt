/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.core.model.siecomp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import org.threeten.bp.ZonedDateTime

@Entity(indices = [
    Index(value = ["uuid"], unique = true)
])
data class Session(
    @SerializedName(value = "id")
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