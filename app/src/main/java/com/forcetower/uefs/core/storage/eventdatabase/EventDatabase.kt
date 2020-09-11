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

package com.forcetower.uefs.core.storage.eventdatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.forcetower.uefs.core.model.siecomp.Session
import com.forcetower.uefs.core.model.siecomp.SessionSpeaker
import com.forcetower.uefs.core.model.siecomp.SessionStar
import com.forcetower.uefs.core.model.siecomp.SessionTag
import com.forcetower.uefs.core.model.siecomp.Speaker
import com.forcetower.uefs.core.model.siecomp.Tag
import com.forcetower.uefs.core.model.unes.AccessToken
import com.forcetower.uefs.core.storage.eventdatabase.dao.AccessTokenDao
import com.forcetower.uefs.core.storage.eventdatabase.dao.EventDao
import com.forcetower.uefs.core.util.Converters

@Database(
    entities = [
        AccessToken::class,
        Session::class,
        Tag::class,
        Speaker::class,
        SessionTag::class,
        SessionSpeaker::class,
        SessionStar::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(value = [Converters::class])
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun accessTokenDao(): AccessTokenDao
}
