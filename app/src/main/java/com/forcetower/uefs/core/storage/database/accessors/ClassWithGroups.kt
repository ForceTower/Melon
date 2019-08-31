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

package com.forcetower.uefs.core.storage.database.accessors

import androidx.room.Embedded
import androidx.room.Relation
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Grade

class ClassWithGroups {
    @Embedded
    lateinit var clazz: Class
    @Relation(parentColumn = "uid", entityColumn = "class_id", entity = ClassGroup::class)
    lateinit var groups: List<ClassGroup>
    @Relation(parentColumn = "uid", entityColumn = "class_id", entity = Grade::class)
    lateinit var grades: List<Grade>
    @Relation(parentColumn = "discipline_id", entityColumn = "uid", entity = Discipline::class)
    lateinit var disciplines: List<Discipline>

    fun discipline() = disciplines[0]
}