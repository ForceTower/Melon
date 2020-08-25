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
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.core.model.unes.FlowchartSemester

@Dao
interface FlowchartDisciplineDao {
    @Query("SELECT fd.id AS id, fd.type AS type, fd.mandatory AS mandatory, fd.completed AS completed, fd.participating AS participating, d.name AS name, d.code AS code, d.credits AS credits, d.department AS department, d.resume AS program FROM FlowchartDiscipline fd INNER JOIN Discipline d ON d.uid = fd.disciplineId WHERE fd.semesterId = :semesterId")
    fun getDecoratedList(semesterId: Long): LiveData<List<FlowchartDisciplineUI>>
    @Query("SELECT fd.id AS id, fd.type AS type, fd.mandatory AS mandatory, fd.completed AS completed, fd.participating AS participating, d.name AS name, d.code AS code, d.credits AS credits, d.department AS department, d.resume AS program FROM FlowchartDiscipline fd INNER JOIN Discipline d ON d.uid = fd.disciplineId WHERE fd.id = :disciplineId")
    fun getDecorated(disciplineId: Long): LiveData<FlowchartDisciplineUI>
    @Query("SELECT s.* FROM FlowchartSemester s INNER JOIN FlowchartDiscipline d ON d.semesterId = s.id WHERE d.id = :disciplineId GROUP BY s.name LIMIT 1")
    fun getSemesterFromDiscipline(disciplineId: Long): LiveData<FlowchartSemester?>
}
