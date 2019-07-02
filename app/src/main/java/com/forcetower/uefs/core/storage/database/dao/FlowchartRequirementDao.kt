package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.forcetower.uefs.core.model.unes.FlowchartRequirementUI

@Dao
interface FlowchartRequirementDao {
    @Query("select FR.id as id, FR.type as type, FR.disciplineId as disciplineId, FR.typeId as typeId, D.name as shownName, FR.requiredDisciplineId as requiredDisciplineId, FR.coursePercentage as coursePercentage, FR.courseHours as courseHours, FS.'order' as sequence from FlowchartRequirement FR left join FlowchartDiscipline FD on FR.requiredDisciplineId = FD.id left join Discipline D on FD.disciplineId = D.uid inner join FlowchartSemester FS on FD.semesterId = FS.id where fr.disciplineId = :disciplineId")
    fun getDecoratedList(disciplineId: Long): LiveData<List<FlowchartRequirementUI>>

    @Query("select FR.id as id, FR.type as type, FR.disciplineId as disciplineId, FR.typeId as typeId, D.name as shownName, FR.requiredDisciplineId as requiredDisciplineId, FR.coursePercentage as coursePercentage, FR.courseHours as courseHours, FS.'order' as sequence from FlowchartRequirement FR inner join FlowchartDiscipline FD on FR.requiredDisciplineId = FD.id inner join Discipline D on FD.disciplineId = D.uid inner join FlowchartSemester FS on FD.semesterId = FS.id where fr.disciplineId = :disciplineId")
    fun getDecoratedListDirect(disciplineId: Long): List<FlowchartRequirementUI>

    @Query("select FR.id as id, FR.type as type, FR.disciplineId as disciplineId, FR.typeId as typeId, D.name as shownName, FR.requiredDisciplineId as requiredDisciplineId, FR.coursePercentage as coursePercentage, FR.courseHours as courseHours, FS.'order' as sequence from FlowchartRequirement FR inner join FlowchartDiscipline FD on FR.disciplineId = FD.id inner join Discipline D on FD.disciplineId = D.uid inner join FlowchartSemester FS on FD.semesterId = FS.id where fr.requiredDisciplineId = :disciplineId order by sequence")
    fun getDecoratedDependenciesDirect(disciplineId: Long): List<FlowchartRequirementUI>
}
