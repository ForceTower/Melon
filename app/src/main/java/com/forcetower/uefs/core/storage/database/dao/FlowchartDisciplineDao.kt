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
