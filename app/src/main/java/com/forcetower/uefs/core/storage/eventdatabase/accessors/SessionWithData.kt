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

package com.forcetower.uefs.core.storage.eventdatabase.accessors

import androidx.room.Embedded
import androidx.room.Relation
import com.forcetower.uefs.core.model.siecomp.Session
import com.forcetower.uefs.core.model.siecomp.SessionSpeaker
import com.forcetower.uefs.core.model.siecomp.SessionStar
import com.forcetower.uefs.core.model.siecomp.SessionTag

class SessionWithData : Comparable<SessionWithData> {
    @Embedded
    lateinit var session: Session
    @Relation(entityColumn = "session_id", parentColumn = "uid", entity = SessionSpeaker::class)
    lateinit var speakersRel: List<SessionSpeakerTalker>
    @Relation(entityColumn = "session_id", parentColumn = "uid", entity = SessionTag::class)
    lateinit var displayTags: List<SessionTagged>
    @Relation(entityColumn = "session_id", parentColumn = "uid")
    lateinit var stars: List<SessionStar>

    fun tags() = displayTags.map { it.singleTag() }.filter { !it.internal }
    fun speakers() = speakersRel.map { it.singleSpeaker() }
    fun isStarred() = stars.isNotEmpty()

    override fun compareTo(other: SessionWithData): Int {
        return session.compareTo(other.session)
    }
}
