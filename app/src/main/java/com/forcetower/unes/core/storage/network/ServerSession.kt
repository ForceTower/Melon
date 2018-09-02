/*
 * Copyright (c) 2018.
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

package com.forcetower.unes.core.storage.network

import com.forcetower.unes.core.model.event.*
import com.google.gson.annotations.SerializedName
import org.threeten.bp.ZonedDateTime

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

    val tags: List<Tag>,
    val speakers: List<Speaker>
) {
    fun toSession() = Session(uid, day, startTime, endTime, title, room, abstract, photoUrl, uuid)

    override fun toString() = "$uid _ $day: $title\n[$speakers]\n[$tags]"

    companion object {
        fun from(session: Session, tags: List<Tag>, speakers: List<Speaker>): ServerSession {
            return ServerSession(
                    session.uid, session.day, session.startTime, session.endTime,
                    session.title, session.room, session.abstract, session.photoUrl,
                    session.uuid, tags, speakers
            )
        }
    }
}