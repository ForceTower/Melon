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

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.forcetower.sagres.database.model.SagresDisciplineGroup
import com.forcetower.uefs.core.model.service.ClassStatsData
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.ClassItem
import com.forcetower.uefs.core.model.unes.ClassMaterial
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.storage.database.aggregation.ClassGroupWithData
import timber.log.Timber

@Dao
abstract class ClassGroupDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(group: ClassGroup): Long

    @Transaction
    open fun defineGroups(groups: List<SagresDisciplineGroup>, notifyMaterials: Boolean = true) {
        for (group in groups) {
            val inserted = insert(group)
            if (inserted != null) {
                Timber.d("Group id: ${inserted.uid}")
                Timber.d("Group Code: ${group.code} has ${group.classItems.sumBy { it.materials.size }} materials")
                for (classItem in group.classItems) {
                    val insertedUid = inserted.uid
                    val item = ClassItem.createFromSagres(insertedUid, classItem)
                    val currentItem = getClassItemDirect(insertedUid, classItem.number)
                    val classId = if (currentItem == null) {
                        insert(item)
                    } else {
                        currentItem.selectiveCopy(item)
                        update(currentItem)
                        currentItem.uid
                    }
                    for (classMaterial in classItem.materials) {
                        val material = ClassMaterial.createFromSagres(inserted.uid, classId, classMaterial, !notifyMaterials)
                        insert(material)
                    }
                }
            }
        }
    }

    @Update(onConflict = IGNORE)
    abstract fun update(item: ClassItem)

    @Query("SELECT * FROM ClassItem WHERE group_id = :groupId AND number = :number")
    abstract fun getClassItemDirect(groupId: Long, number: Int): ClassItem?

    @Transaction
    open fun insert(grp: SagresDisciplineGroup): ClassGroup? {
        val sgr = if (grp.group == null) "unique" else grp.group
        val discipline = selectDisciplineDirect(grp.code.trim())
        var group = selectGroupDirect(grp.semester.trim(), grp.code.trim(), sgr!!)
        val grops = selectGroupsDirect(grp.semester.trim(), grp.code.trim())

        if (grops.isNotEmpty() && grops[0].group.equals("unique", ignoreCase = true)) {
            group = grops[0]
        } else if (grp.group == null && grops.isNotEmpty()) {
            group = grops[0]
        } else if (group == null) {
            Timber.d("Class will be found or will return null")
            val clazz = selectClassDirect(grp.semester.trim(), grp.code.trim())
            Timber.d("Clazz found: $clazz")
            clazz ?: return null

            group = ClassGroup(classId = clazz.uid, group = sgr)
            group.uid = insert(group)
        }

        group.selectiveCopy(grp)
        update(group)

        if (!grp.department.isNullOrBlank()) {
            discipline.department = grp.department!!.trim()
            updateDiscipline(discipline)
            Timber.d("Updated discipline ${discipline.code} department to ${discipline.department}")
        }
        Timber.d("Group insertion completed with $group")
        return group
    }

    open suspend fun insertNewWay(group: ClassGroup): Long {
        val groups = selectGroupsFromClassDirect(group.classId)
        when {
            // just insert, you are brand new
            groups.isEmpty() -> {
                return insert(group)
            }
            groups.size == 1 -> {
                val current = groups.first()
                // are we merging?
                return if (current.group.equals("unique", ignoreCase = true) || current.group.equals(group.group, ignoreCase = true)) {
                    // yes, we are
                    update(group.copy(uid = current.uid, ignored = current.ignored))
                    current.uid
                } else {
                    // no, we are not, you new
                    insert(group)
                }
            }
            // there are plenty of groups here... find the one that fits
            else -> {
                val current = groups.firstOrNull { it.group == "unique" || it.group.equals(group.group, ignoreCase = true) }
                return if (current != null) {
                    // merge the fitter
                    update(group.copy(uid = current.uid, ignored = current.ignored))
                    current.uid
                } else {
                    // no one fits
                    insert(group)
                }
            }
        }
    }

    @Query("SELECT * FROM ClassGroup WHERE class_id = :classId")
    abstract suspend fun selectGroupsFromClassDirect(classId: Long): List<ClassGroup>

    @Update
    abstract fun update(group: ClassGroup)

    @Transaction
    @Query("SELECT * FROM ClassGroup WHERE uid = :classGroupId")
    abstract fun getWithRelations(classGroupId: Long): LiveData<ClassGroupWithData?>

    @Transaction
    @Query("SELECT * FROM ClassGroup WHERE uid = :classGroupId")
    abstract fun getWithRelationsDirect(classGroupId: Long): ClassGroupWithData?

    @Query("SELECT * FROM ClassGroup WHERE uid = :classGroupId")
    abstract suspend fun getGroupDirect(classGroupId: Long): ClassGroup?

    @Query("SELECT c.uid as identifier, gd.date as eval_date, gd.grade as eval_grade, gd.name as eval_name, d.code as code, d.credits as credits, s.sagres_id as semester, s.codename as semester_name, cg.teacher as teacher, cg.teacherEmail as teacherEmail, cg.`group` as `group`, c.final_score as grade, c.partial_score as partialScore, d.name as discipline FROM ClassGroup cg, Class c, Discipline d, Semester s LEFT JOIN Grade gd ON c.uid = gd.class_id WHERE cg.class_id = c.uid AND c.discipline_id = d.uid AND c.semester_id = s.uid AND cg.teacher IS NOT NULL AND cg.`group` IS NOT NULL AND cg.`group` IS NOT 'unique'")
    abstract fun getClassStatsWithAllDirect(): List<ClassStatsData>

    @Query("SELECT c.uid as identifier, gd.date as eval_date, gd.grade as eval_grade, gd.name as eval_name, d.code as code, d.credits as credits, s.sagres_id as semester, s.codename as semester_name, cg.teacher as teacher, cg.teacherEmail as teacherEmail, cg.`group` as `group`, c.final_score as grade, c.partial_score as partialScore, d.name as discipline FROM ClassGroup cg, Class c, Discipline d, Semester s LEFT JOIN Grade gd ON c.uid = gd.class_id WHERE cg.class_id = c.uid AND c.discipline_id = d.uid AND c.semester_id = s.uid AND s.sagres_id = :semesterId AND cg.teacher IS NOT NULL AND cg.`group` IS NOT NULL AND cg.`group` IS NOT 'unique'")
    abstract fun getClassStatsWithAllDirect(semesterId: Long): List<ClassStatsData>

    @Insert(onConflict = IGNORE)
    protected abstract fun insert(item: ClassItem): Long

    @Insert(onConflict = IGNORE)
    protected abstract fun insert(material: ClassMaterial)

    @Query("SELECT g.* FROM ClassGroup g, Class c, Semester s, Discipline d WHERE g.class_id = c.uid AND c.discipline_id = d.uid AND c.semester_id = s.uid AND s.codename = :semester AND LOWER(d.code) = LOWER(:code) AND g.`group` = :group")
    protected abstract fun selectGroupDirect(semester: String, code: String, group: String): ClassGroup?

    @Query("SELECT g.* FROM ClassGroup g, Class c, Semester s, Discipline d WHERE g.class_id = c.uid AND c.discipline_id = d.uid AND c.semester_id = s.uid AND s.codename = :semester AND LOWER(d.code) = LOWER(:code)")
    protected abstract fun selectGroupsDirect(semester: String, code: String): List<ClassGroup>

    @Query("SELECT g.* FROM ClassGroup g, Class c, Semester s, Discipline d WHERE g.class_id = c.uid AND c.discipline_id = d.uid AND c.semester_id = s.uid AND s.codename = :semester AND LOWER(d.code) = LOWER(:code) AND g.`group` = :group")
    abstract fun selectGroup(semester: String, code: String, group: String): LiveData<ClassGroup?>

    @Query(
        "SELECT c.* FROM Class c, Semester s, Discipline d WHERE " +
            "c.discipline_id = d.uid AND " +
            "c.semester_id = s.uid AND " +
            "s.codename = :semester AND " +
            "LOWER(d.code) = LOWER(:code)"
    )
    protected abstract fun selectClassDirect(semester: String, code: String): Class?

    @Query("SELECT * FROM Discipline WHERE LOWER(code) = LOWER(:code)")
    protected abstract fun selectDisciplineDirect(code: String): Discipline

    @Update
    protected abstract fun updateDiscipline(discipline: Discipline)

    @WorkerThread
    @Query("SELECT * FROM ClassGroup WHERE class_id = :classId")
    abstract fun getGroupsFromClassDirect(classId: Long): List<ClassGroup>

    @Query("SELECT * FROM ClassGroup WHERE sagresId = :id")
    abstract suspend fun getByElementalIdDirect(id: Long): ClassGroup?
}
