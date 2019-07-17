/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
import com.forcetower.sagres.database.model.SDisciplineGroup
import com.forcetower.uefs.core.model.service.ClassStatsData
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.ClassItem
import com.forcetower.uefs.core.model.unes.ClassMaterial
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.storage.database.accessors.GroupWithClass
import timber.log.Timber

@Dao
abstract class ClassGroupDao {
    @Insert(onConflict = IGNORE)
    abstract fun insert(group: ClassGroup): Long

    @Transaction
    open fun defineGroups(groups: List<SDisciplineGroup>, notifyMaterials: Boolean = true) {
        for (group in groups) {
            val inserted = insert(group)
            if (inserted != null && group.classItems != null) {
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
    open fun insert(grp: SDisciplineGroup): ClassGroup? {
        val sgr = if (grp.group == null) "unique" else grp.group
        val discipline = selectDisciplineDirect(grp.code.trim())
        var group = selectGroupDirect(grp.semester.trim(), grp.code.trim(), sgr)
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
            discipline.department = grp.department.trim()
            updateDiscipline(discipline)
            Timber.d("Updated discipline ${discipline.code} department to ${discipline.department}")
        }
        Timber.d("Group insertion completed with $group")
        return group
    }

    @Update
    abstract fun update(group: ClassGroup)

    @Transaction
    @Query("SELECT * FROM ClassGroup WHERE uid = :classGroupId")
    abstract fun getWithRelations(classGroupId: Long): LiveData<GroupWithClass?>

    @Transaction
    @Query("SELECT * FROM ClassGroup WHERE uid = :classGroupId")
    abstract fun getWithRelationsDirect(classGroupId: Long): GroupWithClass?

    @Query("SELECT c.uid as identifier, gd.date as eval_date, gd.grade as eval_grade, gd.name as eval_name, d.code as code, d.credits as credits, s.sagres_id as semester, s.codename as semester_name, cg.teacher as teacher, cg.`group` as `group`, c.final_score as grade, c.partial_score as partialScore, d.name as discipline FROM ClassGroup cg, Class c, Discipline d, Semester s, Grade gd WHERE c.uid = gd.class_id AND cg.class_id = c.uid AND c.discipline_id = d.uid AND c.semester_id = s.uid AND cg.teacher IS NOT NULL AND cg.`group` IS NOT NULL")
    abstract fun getClassStatsWithAllDirect(): List<ClassStatsData>

    @Query("SELECT c.uid as identifier, gd.date as eval_date, gd.grade as eval_grade, gd.name as eval_name, d.code as code, d.credits as credits, s.sagres_id as semester, s.codename as semester_name, cg.teacher as teacher, cg.`group` as `group`, c.final_score as grade, c.partial_score as partialScore, d.name as discipline FROM ClassGroup cg, Class c, Discipline d, Semester s, Grade gd WHERE c.uid = gd.class_id AND cg.class_id = c.uid AND c.discipline_id = d.uid AND c.semester_id = s.uid AND s.sagres_id = :semesterId AND cg.teacher IS NOT NULL AND cg.`group` IS NOT NULL")
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

    @Query("SELECT c.* FROM Class c, Semester s, Discipline d WHERE " +
            "c.discipline_id = d.uid AND " +
            "c.semester_id = s.uid AND " +
            "s.codename = :semester AND " +
            "LOWER(d.code) = LOWER(:code)")
    protected abstract fun selectClassDirect(semester: String, code: String): Class?

    @Query("SELECT * FROM Discipline WHERE LOWER(code) = LOWER(:code)")
    protected abstract fun selectDisciplineDirect(code: String): Discipline

    @Update
    protected abstract fun updateDiscipline(discipline: Discipline)

    @WorkerThread
    @Query("SELECT * FROM ClassGroup WHERE class_id = :classId")
    abstract fun getGroupsFromClassDirect(classId: Long): List<ClassGroup>
}