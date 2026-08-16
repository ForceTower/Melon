package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.forcetower.melon.core.database.entity.CurriculumEntity
import dev.forcetower.melon.core.database.entity.CurriculumEntryEntity
import dev.forcetower.melon.core.database.entity.CurriculumPrerequisiteEntity
import dev.forcetower.melon.core.database.entity.CurriculumProgressEntity
import dev.forcetower.melon.core.database.entity.CurriculumRequirementEntity
import kotlinx.coroutines.flow.Flow

// The curriculum mirror behind "Progresso do curso" and the fluxograma. The
// student is bound to exactly one curriculum version at a time, so every table
// here holds that one version's rows and the queries need no scoping — the
// invariant is enforced by [replace], which is the only writer.
@Dao
abstract class CurriculumDao {
    @Query("SELECT * FROM CurriculumProgress WHERE `key` = '${CurriculumProgressEntity.CURRENT}'")
    abstract fun observeProgress(): Flow<CurriculumProgressEntity?>

    @Query("SELECT * FROM Curriculum LIMIT 1")
    abstract fun observeCurriculum(): Flow<CurriculumEntity?>

    @Query("SELECT * FROM CurriculumRequirement ORDER BY position ASC")
    abstract fun observeRequirements(): Flow<List<CurriculumRequirementEntity>>

    @Query("SELECT * FROM CurriculumEntry ORDER BY position ASC")
    abstract fun observeEntries(): Flow<List<CurriculumEntryEntity>>

    @Query("SELECT * FROM CurriculumPrerequisite ORDER BY position ASC")
    abstract fun observePrerequisites(): Flow<List<CurriculumPrerequisiteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertProgress(progress: CurriculumProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCurriculum(curriculum: CurriculumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRequirements(requirements: List<CurriculumRequirementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertEntries(entries: List<CurriculumEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPrerequisites(prerequisites: List<CurriculumPrerequisiteEntity>)

    @Query("DELETE FROM CurriculumProgress")
    abstract suspend fun clearProgress()

    // Requirements, entries and edges all cascade off `Curriculum`.
    @Query("DELETE FROM Curriculum")
    abstract suspend fun clearCurriculum()

    // One refresh result written whole, in one transaction: a mid-run failure
    // leaves the previous payload intact instead of a half-written grid, and
    // observers wake once, on a consistent snapshot. `curriculum` is null when
    // the portal knows the student's hours but we hold no grid for the course
    // — the progress row still lands, so the screen can show hours with no
    // denominator.
    @Transaction
    open suspend fun replace(
        progress: CurriculumProgressEntity,
        curriculum: CurriculumEntity?,
        requirements: List<CurriculumRequirementEntity>,
        entries: List<CurriculumEntryEntity>,
        prerequisites: List<CurriculumPrerequisiteEntity>,
    ) {
        clearProgress()
        clearCurriculum()
        insertProgress(progress)
        if (curriculum == null) return
        insertCurriculum(curriculum)
        if (requirements.isNotEmpty()) insertRequirements(requirements)
        if (entries.isNotEmpty()) insertEntries(entries)
        if (prerequisites.isNotEmpty()) insertPrerequisites(prerequisites)
    }

    @Transaction
    open suspend fun clear() {
        clearProgress()
        clearCurriculum()
    }
}
