package com.forcetower.uefs.core.storage.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.forcetower.uefs.core.model.unes.FlowchartRequirementUI

@Dao
interface FlowchartRequirementDao {
    @Query("select FR.id as id, FR.type as type, D.name as shownName, FR.requiredDisciplineId as requiredDisciplineId, FR.coursePercentage as coursePercentage, FR.courseHours as courseHours from FlowchartRequirement FR left join FlowchartDiscipline FD on FR.requiredDisciplineId = FD.id left join Discipline D on FD.disciplineId = D.uid where fr.disciplineId = :disciplineId")
    fun getDecoratedList(disciplineId: Long): LiveData<List<FlowchartRequirementUI>>
}
