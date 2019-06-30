package com.forcetower.uefs.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.uefs.core.model.service.FlowchartDTO
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Flowchart
import com.forcetower.uefs.core.model.unes.FlowchartDiscipline
import com.forcetower.uefs.core.model.unes.FlowchartRequirement
import com.forcetower.uefs.core.model.unes.FlowchartSemester

@Dao
abstract class FlowchartDao {
    @Transaction
    open fun insertFromNetwork(data: FlowchartDTO) {
        val flowchart = data.toFlowchart()
        insertFlowchart(flowchart)

        val semesters = data.semesters.map { it.toSemester() }
        insertSemesters(semesters)

        val requirements = mutableListOf<FlowchartRequirement>()

        data.semesters.forEach { semester ->
            semester.disciplines.forEach { discipline ->
                val localDiscipline = discipline.toLocalDiscipline()
                val localId = findLocalDiscipline(localDiscipline.code) ?: insertLocalDiscipline(localDiscipline)
                val flowDiscipline = discipline.toDiscipline(localId, semester.id)
                insertDiscipline(flowDiscipline)
                requirements.addAll(discipline.requirements)
            }
        }

        insertRequirements(requirements)
    }

    @Insert(onConflict = REPLACE)
    protected abstract fun insertFlowchart(flowchart: Flowchart)

    @Insert(onConflict = REPLACE)
    protected abstract fun insertSemesters(semesters: List<FlowchartSemester>)

    @Insert(onConflict = REPLACE)
    protected abstract fun insertDiscipline(discipline: FlowchartDiscipline)

    @Insert(onConflict = REPLACE)
    protected abstract fun insertRequirements(requirements: List<FlowchartRequirement>)

    @Insert(onConflict = REPLACE)
    protected abstract fun insertLocalDiscipline(discipline: Discipline): Long

    @Query("SELECT uid from Discipline WHERE code = :code LIMIT 1")
    protected abstract fun findLocalDiscipline(code: String): Long?
}
