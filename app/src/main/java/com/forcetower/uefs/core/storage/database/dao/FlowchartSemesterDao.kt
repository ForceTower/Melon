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

package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.forcetower.uefs.core.model.unes.FlowchartSemesterUI

@Dao
interface FlowchartSemesterDao {
    @Query("SELECT s.id as id, s.name as name, s.`order` as `order`, f.id as flowchartId, SUM(d.credits) as hours, COUNT(d.uid) as disciplines FROM FlowchartSemester s INNER JOIN Flowchart f ON f.id = s.flowchartId INNER JOIN FlowchartDiscipline fd on fd.semesterId = s.id INNER JOIN Discipline d on d.uid = fd.disciplineId WHERE f.courseId = :courseId GROUP BY s.id")
    fun getDecoratedList(courseId: Long): LiveData<List<FlowchartSemesterUI>>

    @Query("SELECT s.id as id, s.name as name, s.`order` as `order`, f.id as flowchartId, SUM(d.credits) as hours, COUNT(d.uid) as disciplines FROM FlowchartSemester s INNER JOIN Flowchart f ON f.id = s.flowchartId INNER JOIN FlowchartDiscipline fd on fd.semesterId = s.id INNER JOIN Discipline d on d.uid = fd.disciplineId WHERE s.id = :semesterId GROUP BY s.id LIMIT 1")
    fun getDecorated(semesterId: Long): LiveData<FlowchartSemesterUI>
}
