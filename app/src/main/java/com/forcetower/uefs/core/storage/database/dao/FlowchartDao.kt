package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
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
import java.util.Calendar

@Dao
abstract class FlowchartDao {
    @Query("SELECT * FROM Flowchart")
    abstract fun getFlowcharts(): LiveData<List<Flowchart>>

    @Insert(onConflict = REPLACE)
    protected abstract fun insert(list: Flowchart)

    @Transaction
    open fun insertFlowcharts(list: List<Flowchart>) {
        val now = Calendar.getInstance().timeInMillis
        list.forEach {
            val found = findFlowchart(it.id) != null
            if (!found) {
                Flowchart(it.id, it.courseId, it.description, now)
            } else {
                updateFlowchart(it.id, it.description, now)
            }
        }
    }

    @Query("SELECT * FROM Flowchart WHERE id = :id LIMIT 1")
    abstract fun findFlowchart(id: Long): Flowchart?

    @Query("UPDATE Flowchart SET description = :description, lastUpdated = :lastUpdated WHERE id = :id")
    abstract fun updateFlowchart(id: Long, description: String, lastUpdated: Long)

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
                val localRetrieved = findLocalDiscipline(localDiscipline.code)
                val disciplineId = if (localRetrieved != null) {
                    val localId = localRetrieved.uid
                    if (discipline.program != null) {
                        updateDisciplineResume(localId, discipline.program)
                    }
                    if (localRetrieved.department == null) {
                        updateDisciplineDepartment(localId, discipline.department)
                    }
                    if (localRetrieved.credits != discipline.credits) {
                        updateDisciplineCredits(localId, discipline.credits)
                    }
                    localId
                } else {
                    insertLocalDiscipline(localDiscipline)
                }
                val flowDiscipline = discipline.toDiscipline(disciplineId, semester.id)
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

    @Query("SELECT * from Discipline WHERE LOWER(code) = LOWER(:code) LIMIT 1")
    protected abstract fun findLocalDiscipline(code: String): Discipline?

    @Query("UPDATE Discipline SET resume = :program WHERE uid = :id")
    protected abstract fun updateDisciplineResume(id: Long, program: String)

    @Query("UPDATE Discipline SET department = :department WHERE uid = :id")
    protected abstract fun updateDisciplineDepartment(id: Long, department: String)

    @Query("UPDATE Discipline SET credits = :credits WHERE uid = :id")
    abstract fun updateDisciplineCredits(id: Long, credits: Int)
}
