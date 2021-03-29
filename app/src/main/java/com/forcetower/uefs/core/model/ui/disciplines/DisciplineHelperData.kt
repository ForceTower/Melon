/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.core.model.ui.disciplines

import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup

sealed class DisciplineHelperData {
    class Header(val clazz: ClassFullWithGroup) : DisciplineHelperData()
    class Score(val clazz: ClassFullWithGroup, val grade: Grade) : DisciplineHelperData()
    class Final(val clazz: ClassFullWithGroup) : DisciplineHelperData()
    class Mean(val clazz: ClassFullWithGroup) : DisciplineHelperData()
    class GroupingName(val clazz: ClassFullWithGroup, val name: String) : DisciplineHelperData()
    class EmptySemester(val semester: Semester) : DisciplineHelperData()
    object Divider : DisciplineHelperData()
}
