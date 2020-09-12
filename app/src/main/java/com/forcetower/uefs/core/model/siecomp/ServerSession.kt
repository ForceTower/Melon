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

import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime

data class ServerSession(
    @SerializedName(value = "id")
    val uid: Long,
    val day: Int,
    @SerializedName("start_time")
    val startTime: ZonedDateTime,
    @SerializedName("end_time")
    val endTime: ZonedDateTime,
    val title: String,
    val room: String,
    val abstract: String,
    @SerializedName("photo_url")
    val photoUrl: String,
    val uuid: String,
    val type: Int,

    val tags: List<Tag>,
    val speakers: List<Speaker>
) {
    fun toSession() = Session(uid, day, startTime, endTime, title, room, abstract, photoUrl, uuid, type)

    override fun toString() = "$uid _ $day: $title\n[$speakers]\n[$tags]"

    companion object {
        fun from(session: Session, tags: List<Tag>, speakers: List<Speaker>): ServerSession {
            return ServerSession(
                session.uid, session.day, session.startTime, session.endTime,
                session.title, session.room, session.resume, session.photoUrl,
                session.uuid, session.type, tags, speakers
            )
        }
    }
}
