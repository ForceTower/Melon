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

package com.forcetower.uefs.core.storage.eventdatabase.accessors

import androidx.room.Embedded
import androidx.room.Relation
import com.forcetower.uefs.core.model.siecomp.Session
import com.forcetower.uefs.core.model.siecomp.SessionSpeaker
import com.forcetower.uefs.core.model.siecomp.SessionStar
import com.forcetower.uefs.core.model.siecomp.SessionTag

class SessionWithData: Comparable<SessionWithData>{
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