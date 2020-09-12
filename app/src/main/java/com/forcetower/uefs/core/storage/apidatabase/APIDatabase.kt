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

package com.forcetower.uefs.core.storage.apidatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import com.forcetower.uefs.core.model.api.UDiscipline
import com.forcetower.uefs.core.model.api.UTeacher
import com.forcetower.uefs.core.storage.apidatabase.dao.DisciplineDao
import com.forcetower.uefs.core.storage.apidatabase.dao.TeacherDao

@Database(
    entities = [
        UDiscipline::class,
        UTeacher::class
    ],
    version = 1,
    exportSchema = true
)
abstract class APIDatabase : RoomDatabase() {
    abstract fun disciplineDao(): DisciplineDao
    abstract fun teacherDao(): TeacherDao
}
