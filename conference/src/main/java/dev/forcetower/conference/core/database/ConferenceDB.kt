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

package dev.forcetower.conference.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.forcetower.conference.core.database.dao.SessionDao
import dev.forcetower.conference.core.model.persistence.Conference
import dev.forcetower.conference.core.model.persistence.ConferenceDay
import dev.forcetower.conference.core.model.persistence.Session
import dev.forcetower.conference.core.model.persistence.SessionSpeaker
import dev.forcetower.conference.core.model.persistence.SessionTag
import dev.forcetower.conference.core.model.persistence.Speaker
import dev.forcetower.conference.core.model.persistence.Tag

@Database(
    entities = [
        Conference::class,
        ConferenceDay::class,
        Session::class,
        Speaker::class,
        Tag::class,
        SessionSpeaker::class,
        SessionTag::class
    ],
    version = 1
)
@TypeConverters(value = [Converters::class])
abstract class ConferenceDB : RoomDatabase() {
    abstract fun sessions(): SessionDao
}
