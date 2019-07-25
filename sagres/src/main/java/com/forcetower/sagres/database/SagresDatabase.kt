/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.sagres.database

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.forcetower.sagres.database.dao.AccessDao
import com.forcetower.sagres.database.dao.ClazzDao
import com.forcetower.sagres.database.dao.DisciplineResumedDao
import com.forcetower.sagres.database.dao.MessageScopeDao
import com.forcetower.sagres.database.dao.PersonDao
import com.forcetower.sagres.database.model.SAccess
import com.forcetower.sagres.database.model.SClass
import com.forcetower.sagres.database.model.SDisciplineResumed
import com.forcetower.sagres.database.model.SMessageScope
import com.forcetower.sagres.database.model.SPerson

@Database(entities = [
    SAccess::class,
    SPerson::class,
    SMessageScope::class,
    SClass::class,
    SDisciplineResumed::class
], version = 4, exportSchema = true)
abstract class SagresDatabase : RoomDatabase() {
    abstract fun accessDao(): AccessDao
    abstract fun personDao(): PersonDao
    abstract fun messageScopeDao(): MessageScopeDao
    abstract fun clazzDao(): ClazzDao
    abstract fun disciplineDao(): DisciplineResumedDao

    companion object {
        private const val DB_NAME = "unesx_sagres_database.db"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun create(context: Context): SagresDatabase {
            return Room.databaseBuilder(context, SagresDatabase::class.java, DB_NAME)
                .addMigrations(M1TO2, M2TO3, M3TO4)
                .allowMainThreadQueries()
                .build()
        }
    }
}
