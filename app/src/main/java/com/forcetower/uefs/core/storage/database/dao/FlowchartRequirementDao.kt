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
import com.forcetower.uefs.core.model.unes.FlowchartRequirementUI
import timber.log.Timber

@Dao
abstract class FlowchartRequirementDao {
    @Query("select FR.id as id, FR.type as type, FR.disciplineId as disciplineId, FR.typeId as typeId, D.name as shownName, FR.requiredDisciplineId as requiredDisciplineId, FR.coursePercentage as coursePercentage, FR.courseHours as courseHours, FS.'order' as sequence, FS.name as semesterName, FD.completed as completed from FlowchartRequirement FR left join FlowchartDiscipline FD on FR.requiredDisciplineId = FD.id left join Discipline D on FD.disciplineId = D.uid left join FlowchartSemester FS on FD.semesterId = FS.id where fr.disciplineId = :disciplineId")
    abstract fun getDecoratedList(disciplineId: Long): LiveData<List<FlowchartRequirementUI>>

    @Query("select FR.id as id, FR.type as type, FR.disciplineId as disciplineId, FR.typeId as typeId, D.name as shownName, FR.requiredDisciplineId as requiredDisciplineId, FR.coursePercentage as coursePercentage, FR.courseHours as courseHours, FS.'order' as sequence, FS.name as semesterName, FD.completed as completed from FlowchartRequirement FR left join FlowchartDiscipline FD on FR.requiredDisciplineId = FD.id left join Discipline D on FD.disciplineId = D.uid left join FlowchartSemester FS on FD.semesterId = FS.id where fr.disciplineId = :disciplineId")
    abstract fun getDecoratedListCommonDirect(disciplineId: Long): List<FlowchartRequirementUI>

    @Query("select FR.id as id, FR.type as type, FR.disciplineId as disciplineId, FR.typeId as typeId, D.name as shownName, FR.requiredDisciplineId as requiredDisciplineId, FR.coursePercentage as coursePercentage, FR.courseHours as courseHours, FS.'order' as sequence, FS.name as semesterName, FD.completed as completed from FlowchartRequirement FR inner join FlowchartDiscipline FD on FR.requiredDisciplineId = FD.id inner join Discipline D on FD.disciplineId = D.uid inner join FlowchartSemester FS on FD.semesterId = FS.id where fr.disciplineId = :disciplineId")
    abstract fun getDecoratedListDirect(disciplineId: Long): List<FlowchartRequirementUI>

    @Query("select FR.id as id, FR.type as type, FR.disciplineId as disciplineId, FR.typeId as typeId, D.name as shownName, FR.requiredDisciplineId as requiredDisciplineId, FR.coursePercentage as coursePercentage, FR.courseHours as courseHours, FS.'order' as sequence, FS.name as semesterName, FD.completed as completed from FlowchartRequirement FR inner join FlowchartDiscipline FD on FR.disciplineId = FD.id inner join Discipline D on FD.disciplineId = D.uid inner join FlowchartSemester FS on FD.semesterId = FS.id where fr.requiredDisciplineId = :disciplineId order by sequence")
    abstract fun getDecoratedDependenciesDirect(disciplineId: Long): List<FlowchartRequirementUI>

    fun getRecursiveRequirementsUI(disciplineId: Long, name: String): List<FlowchartRequirementUI> {
        try {
            return getRecursiveRequirementsWorker(disciplineId)
                .distinctBy { it.requiredDisciplineId }
                .map {
                    FlowchartRequirementUI(
                        it.id,
                        name,
                        it.shownName,
                        it.disciplineId,
                        it.requiredDisciplineId,
                        it.coursePercentage,
                        it.courseHours,
                        -1,
                        it.sequence,
                        it.semesterName,
                        it.completed
                    )
                }
                .sortedWith(
                    Comparator { a, b ->
                        val diff = compareValues(a.sequence, b.sequence)
                        if (diff != 0) diff else compareValues(a.shownName, b.shownName)
                    }
                )
        } catch (t: Throwable) {
            Timber.e(t, "A stack overflow on requirements!")
        }
        return emptyList()
    }

    fun getRecursiveUnlockRequirementUI(disciplineId: Long, name: String): List<FlowchartRequirementUI> {
        try {
            return getRecursiveUnlockRequirementWorker(disciplineId)
                .distinctBy { it.disciplineId }
                .map {
                    FlowchartRequirementUI(
                        it.id,
                        name,
                        it.shownName,
                        it.disciplineId,
                        it.requiredDisciplineId,
                        it.coursePercentage,
                        it.courseHours,
                        -2,
                        it.sequence,
                        it.semesterName,
                        it.completed
                    )
                }
                .sortedWith(
                    Comparator { a, b ->
                        val diff = compareValues(a.sequence, b.sequence)
                        if (diff != 0) diff else compareValues(a.shownName, b.shownName)
                    }
                )
        } catch (t: Throwable) {
            Timber.e(t, "A stack overflow on unlock!")
        }
        return emptyList()
    }

    private fun getRecursiveRequirementsWorker(disciplineId: Long?): List<FlowchartRequirementUI> {
        disciplineId ?: return emptyList()

        val requirements = getDecoratedListDirect(disciplineId).filter {
            it.requiredDisciplineId != null && it.typeId == 1
        }
        Timber.d("Requirements $requirements")
        if (requirements.isEmpty()) return emptyList()

        val iteration = mutableListOf<FlowchartRequirementUI>()
        iteration += requirements

        requirements.forEach {
            val items = getRecursiveRequirementsWorker(it.requiredDisciplineId)
            iteration += items
        }

        return iteration
    }

    private fun getRecursiveUnlockRequirementWorker(disciplineId: Long?): List<FlowchartRequirementUI> {
        disciplineId ?: return emptyList()
        val dependants = getDecoratedDependenciesDirect(disciplineId).filter { it.typeId == 1 }
        if (dependants.isEmpty()) return emptyList()

        val iteration = mutableListOf<FlowchartRequirementUI>()
        iteration += dependants

        dependants.forEach {
            val items = getRecursiveUnlockRequirementWorker(it.disciplineId)
            iteration += items
        }
        return iteration
    }
}
