package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.forcetower.melon.core.database.entity.ClassAllocationEntity
import dev.forcetower.melon.core.database.entity.ClassEntity
import dev.forcetower.melon.core.database.entity.ClassEvaluationEntity
import dev.forcetower.melon.core.database.entity.ClassSpaceEntity
import dev.forcetower.melon.core.database.entity.ClassTeacherEntity
import dev.forcetower.melon.core.database.entity.DisciplineEntity
import dev.forcetower.melon.core.database.entity.DisciplineOfferEntity
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.entity.StudentClassEntity
import dev.forcetower.melon.core.database.entity.StudentGradeEntity
import dev.forcetower.melon.core.database.entity.TeacherEntity

// The single choke point for applying a server semester payload. The
// @Transaction method on applySemesterPayload guarantees the wipe-then-insert
// is atomic — no readers observe the semester mid-apply.
//
// Wipe is scoped: DELETE FROM DisciplineOffer WHERE semesterId = ? cascades to
// Class → ClassTeacher, ClassAllocation, StudentClass → StudentGrade,
// ClassEvaluation. Catalog tables (Discipline, Teacher, ClassSpace) are
// upsert-only because they're shared across semesters.
@Dao
abstract class AcademicDao {
    @Upsert
    abstract suspend fun upsertDisciplines(items: List<DisciplineEntity>)

    @Upsert
    abstract suspend fun upsertTeachers(items: List<TeacherEntity>)

    @Upsert
    abstract suspend fun upsertSpaces(items: List<ClassSpaceEntity>)

    @Upsert
    abstract suspend fun upsertDisciplineOffers(items: List<DisciplineOfferEntity>)

    @Upsert
    abstract suspend fun upsertClasses(items: List<ClassEntity>)

    @Upsert
    abstract suspend fun upsertClassTeachers(items: List<ClassTeacherEntity>)

    @Upsert
    abstract suspend fun upsertAllocations(items: List<ClassAllocationEntity>)

    @Upsert
    abstract suspend fun upsertStudentClasses(items: List<StudentClassEntity>)

    @Upsert
    abstract suspend fun upsertEvaluations(items: List<ClassEvaluationEntity>)

    @Upsert
    abstract suspend fun upsertGrades(items: List<StudentGradeEntity>)

    @Upsert
    abstract suspend fun upsertSemester(semester: SemesterEntity)

    // Cascade-wipes the semester's subtree. FK cascades handle Class ->
    // descendants; the offer table is the upper boundary of "scoped" rows.
    @Query("DELETE FROM DisciplineOffer WHERE semesterId = :semesterId")
    protected abstract suspend fun wipeSemesterSubtree(semesterId: String)

    @Transaction
    open suspend fun applySemesterPayload(
        semesterId: String,
        semester: SemesterEntity,
        disciplines: List<DisciplineEntity>,
        teachers: List<TeacherEntity>,
        spaces: List<ClassSpaceEntity>,
        offers: List<DisciplineOfferEntity>,
        classes: List<ClassEntity>,
        classTeachers: List<ClassTeacherEntity>,
        allocations: List<ClassAllocationEntity>,
        studentClasses: List<StudentClassEntity>,
        evaluations: List<ClassEvaluationEntity>,
        grades: List<StudentGradeEntity>,
    ) {
        wipeSemesterSubtree(semesterId)
        // Catalog first so FKs resolve on the inserts that follow.
        upsertDisciplines(disciplines)
        upsertTeachers(teachers)
        upsertSpaces(spaces)
        // Scoped (re)inserts. Order matches FK topology.
        upsertSemester(semester)
        upsertDisciplineOffers(offers)
        upsertClasses(classes)
        upsertClassTeachers(classTeachers)
        upsertAllocations(allocations)
        upsertStudentClasses(studentClasses)
        upsertEvaluations(evaluations)
        upsertGrades(grades)
    }

    @Query("DELETE FROM Discipline")
    abstract suspend fun clearDisciplines()

    @Query("DELETE FROM Teacher")
    abstract suspend fun clearTeachers()

    @Query("DELETE FROM ClassSpace")
    abstract suspend fun clearSpaces()
}
