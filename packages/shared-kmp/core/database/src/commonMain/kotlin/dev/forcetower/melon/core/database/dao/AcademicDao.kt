package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.forcetower.melon.core.database.entity.ClassAllocationEntity
import dev.forcetower.melon.core.database.entity.ClassEntity
import dev.forcetower.melon.core.database.entity.ClassEvaluationEntity
import dev.forcetower.melon.core.database.entity.ClassLectureEntity
import dev.forcetower.melon.core.database.entity.ClassSpaceEntity
import dev.forcetower.melon.core.database.entity.ClassTeacherEntity
import dev.forcetower.melon.core.database.entity.DisciplineEntity
import dev.forcetower.melon.core.database.entity.DisciplineOfferEntity
import dev.forcetower.melon.core.database.entity.LectureMaterialEntity
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.entity.StudentClassEntity
import dev.forcetower.melon.core.database.entity.StudentGradeEntity
import dev.forcetower.melon.core.database.entity.TeacherEntity
import dev.forcetower.melon.core.database.query.AttendanceSummaryRow
import dev.forcetower.melon.core.database.query.EnrolledDisciplineRow
import dev.forcetower.melon.core.database.query.PartialGradeRow
import dev.forcetower.melon.core.database.query.RecentLectureRow
import dev.forcetower.melon.core.database.query.SemesterAllocationRow
import dev.forcetower.melon.core.database.query.SemesterClassAggregate
import dev.forcetower.melon.core.database.query.StudentDisciplineRow
import dev.forcetower.melon.core.database.query.TodayLectureRow
import dev.forcetower.melon.core.database.query.UpcomingEvaluationRow
import dev.forcetower.melon.core.database.query.WeekLectureRow
import kotlinx.coroutines.flow.Flow

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
    abstract suspend fun upsertLectures(items: List<ClassLectureEntity>)

    @Upsert
    abstract suspend fun upsertLectureMaterials(items: List<LectureMaterialEntity>)

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
        lectures: List<ClassLectureEntity>,
        lectureMaterials: List<LectureMaterialEntity>,
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
        upsertLectures(lectures)
        upsertLectureMaterials(lectureMaterials)
    }

    @Query("DELETE FROM Discipline")
    abstract suspend fun clearDisciplines()

    @Query("DELETE FROM Teacher")
    abstract suspend fun clearTeachers()

    @Query("DELETE FROM ClassSpace")
    abstract suspend fun clearSpaces()

    // Read side: projected queries for UI. Kept alongside the writes so the
    // joins reference the same topology the inserts rely on.

    @Query(
        """
        SELECT COUNT(*) AS classCount, COALESCE(SUM(c.hours), 0) AS totalHours
          FROM Class c
          JOIN DisciplineOffer o ON o.id = c.offerId
         WHERE o.semesterId = :semesterId
        """,
    )
    abstract suspend fun getSemesterAggregate(semesterId: String): SemesterClassAggregate

    // Every allocation (day + time slot) for the semester joined with its
    // discipline, first teacher, and room. Caller picks "next" / "now" from
    // this list — cheap to do in memory vs. SQL-heavy date math that would
    // vary by platform.
    @Query(ALLOCATIONS_SQL)
    abstract suspend fun listSemesterAllocations(semesterId: String): List<SemesterAllocationRow>

    // Reactive variant of [listSemesterAllocations]. Re-emits on any write to
    // the joined tables (allocations, classes, offers, disciplines, teachers,
    // spaces) so Overview's "now" / "today" flows track sync activity.
    @Query(ALLOCATIONS_SQL)
    abstract fun observeSemesterAllocations(semesterId: String): Flow<List<SemesterAllocationRow>>

    // One row per enrolled discipline in the semester. `weightedAverage` is
    // SUM(value × weight) / SUM(weight) across graded rows only — null when no
    // grade has landed. `finalGrade` stays the authoritative "done" grade.
    @Query(
        """
        SELECT d.id AS disciplineId,
               d.code AS code,
               d.name AS name,
               sc.finalGrade AS finalGrade,
               sc.approved AS approved,
               (
                 SELECT SUM(CAST(sg.value AS REAL) * CAST(sg.weight AS REAL))
                        / NULLIF(SUM(CAST(sg.weight AS REAL)), 0)
                   FROM StudentGrade sg
                  WHERE sg.studentClassId = sc.id
                    AND sg.value IS NOT NULL
                    AND sg.value != ''
               ) AS weightedAverage
          FROM StudentClass sc
          JOIN Class c ON c.id = sc.classId
          JOIN DisciplineOffer o ON o.id = c.offerId
          JOIN Discipline d ON d.id = o.disciplineId
         WHERE o.semesterId = :semesterId
         ORDER BY d.code ASC
        """,
    )
    abstract fun observeStudentDisciplines(semesterId: String): Flow<List<StudentDisciplineRow>>

    // Today's lectures, filtered to a semester+date, carrying only the topic
    // (subject). Used to enrich NowCard / Timeline rows with what's being
    // taught in each slot. `date` is an ISO-8601 day string.
    @Query(
        """
        SELECT c.id AS classId,
               cl.subject AS subject
          FROM ClassLecture cl
          JOIN Class c ON c.id = cl.classId
          JOIN DisciplineOffer o ON o.id = c.offerId
         WHERE o.semesterId = :semesterId AND cl.date = :date
        """,
    )
    abstract fun observeTodayLecturesForSemester(
        semesterId: String,
        date: String,
    ): Flow<List<TodayLectureRow>>

    // Closest upcoming evaluation. Heuristic: a StudentGrade row whose date
    // hasn't passed and whose value is still empty is an un-graded future
    // test. Ordered ascending so LIMIT 1 is the nearest one.
    @Query(
        """
        SELECT sg.date AS date,
               e.name AS evaluationName,
               d.code AS disciplineCode,
               d.name AS disciplineName
          FROM StudentGrade sg
          JOIN ClassEvaluation e ON e.id = sg.evaluationId
          JOIN StudentClass sc ON sc.id = sg.studentClassId
          JOIN Class c ON c.id = sc.classId
          JOIN DisciplineOffer o ON o.id = c.offerId
          JOIN Discipline d ON d.id = o.disciplineId
         WHERE o.semesterId = :semesterId
           AND sg.date IS NOT NULL
           AND sg.date >= :today
           AND (sg.value IS NULL OR sg.value = '')
         ORDER BY sg.date ASC
         LIMIT 1
        """,
    )
    abstract fun observeClosestUpcomingEvaluation(
        semesterId: String,
        today: String,
    ): Flow<UpcomingEvaluationRow?>

    // Semester-wide miss/hours aggregate. Percentage (100 - missed/hours*100)
    // and allowed-absences (hours * 0.25) are derived on the client.
    @Query(
        """
        SELECT COALESCE(SUM(sc.missedClasses), 0) AS totalMissed,
               COALESCE(SUM(c.hours), 0) AS totalHours
          FROM StudentClass sc
          JOIN Class c ON c.id = sc.classId
          JOIN DisciplineOffer o ON o.id = c.offerId
         WHERE o.semesterId = :semesterId
        """,
    )
    abstract fun observeAttendanceSummary(semesterId: String): Flow<AttendanceSummaryRow>

    // Most recent N lectures for the semester with their upstream situation
    // code. ViewModel maps situation -> present/absent for the 14-day strip.
    @Query(
        """
        SELECT cl.date AS date,
               cl.situation AS situation
          FROM ClassLecture cl
          JOIN Class c ON c.id = cl.classId
          JOIN DisciplineOffer o ON o.id = c.offerId
         WHERE o.semesterId = :semesterId AND cl.date IS NOT NULL
         ORDER BY cl.date DESC
         LIMIT :limit
        """,
    )
    abstract fun observeRecentLectures(
        semesterId: String,
        limit: Int,
    ): Flow<List<RecentLectureRow>>

    // Lectures (subjects) for every class in the semester that landed within
    // a date range. The Schedule week view reads this to pin each allocation
    // to the lecture topic scheduled for that exact date; the join happens
    // client-side on (classId, date) so the SQL stays cache-friendly.
    @Query(
        """
        SELECT c.id AS classId,
               cl.date AS date,
               cl.subject AS subject
          FROM ClassLecture cl
          JOIN Class c ON c.id = cl.classId
          JOIN DisciplineOffer o ON o.id = c.offerId
         WHERE o.semesterId = :semesterId
           AND cl.date IS NOT NULL
           AND cl.date BETWEEN :start AND :end
        """,
    )
    abstract fun observeLecturesInRange(
        semesterId: String,
        start: String,
        end: String,
    ): Flow<List<WeekLectureRow>>

    // Every StudentClass the signed-in user is enrolled in, joined with its
    // class/offer/discipline and primary teacher. The Disciplines list screen
    // groups these rows by (semesterId, offerId) to produce one card per
    // discipline — disciplines that run multiple groups (e.g. theory +
    // practice) appear as multiple rows here and are aggregated in Kotlin.
    @Query(
        """
        SELECT sc.id AS studentClassId,
               c.id AS classId,
               c.type AS classType,
               c.groupName AS groupName,
               c.hours AS classHours,
               o.id AS offerId,
               o.semesterId AS semesterId,
               COALESCE(o.hours, d.hours) AS disciplineHours,
               d.id AS disciplineId,
               d.code AS disciplineCode,
               d.name AS disciplineName,
               d.department AS department,
               sc.finalGrade AS finalGrade,
               sc.approved AS approved,
               sc.missedClasses AS missedClasses,
               (
                 SELECT t.name FROM Teacher t
                  JOIN ClassTeacher ct ON ct.teacherId = t.id
                  WHERE ct.classId = c.id
                  LIMIT 1
               ) AS teacherName
          FROM StudentClass sc
          JOIN Class c ON c.id = sc.classId
          JOIN DisciplineOffer o ON o.id = c.offerId
          JOIN Discipline d ON d.id = o.disciplineId
         ORDER BY o.semesterId DESC, d.code ASC
        """,
    )
    abstract fun observeAllEnrolledDisciplines(): Flow<List<EnrolledDisciplineRow>>

    // Raw StudentGrade rows across every enrolled class. Paired with
    // [observeAllEnrolledDisciplines] in the use case so evaluation counts,
    // weighted averages, and "next evaluation" all derive from one grouped
    // dataset instead of N subqueries.
    @Query(
        """
        SELECT sg.id AS gradeId,
               sg.studentClassId AS studentClassId,
               sg.name AS name,
               sg.nameShort AS nameShort,
               sg.ordinal AS ordinal,
               sg.weight AS weight,
               sg.value AS value,
               sg.date AS date
          FROM StudentGrade sg
         ORDER BY sg.studentClassId ASC, sg.ordinal ASC
        """,
    )
    abstract fun observeAllPartialGrades(): Flow<List<PartialGradeRow>>

    private companion object {
        const val ALLOCATIONS_SQL = """
            SELECT a.id AS allocationId,
                   c.id AS classId,
                   d.code AS disciplineCode,
                   d.name AS disciplineName,
                   a.day AS day,
                   a.startTime AS startTime,
                   a.endTime AS endTime,
                   s.location AS spaceLocation,
                   s.campus AS spaceCampus,
                   s.modulo AS spaceModulo,
                   (
                     SELECT t.name FROM Teacher t
                      JOIN ClassTeacher ct ON ct.teacherId = t.id
                      WHERE ct.classId = c.id
                      LIMIT 1
                   ) AS teacherName
              FROM ClassAllocation a
              JOIN Class c ON c.id = a.classId
              JOIN DisciplineOffer o ON o.id = c.offerId
              JOIN Discipline d ON d.id = o.disciplineId
              LEFT JOIN ClassSpace s ON s.id = a.spaceId
             WHERE o.semesterId = :semesterId
        """
    }
}
