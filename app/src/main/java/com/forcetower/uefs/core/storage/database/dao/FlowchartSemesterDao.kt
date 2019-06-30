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
