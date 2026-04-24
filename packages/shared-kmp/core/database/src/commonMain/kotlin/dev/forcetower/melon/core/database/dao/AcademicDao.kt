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
import dev.forcetower.melon.core.database.query.DisciplineDetailEnrollmentRow
import dev.forcetower.melon.core.database.query.DisciplineDetailGradeRow
import dev.forcetower.melon.core.database.query.DisciplineDetailLectureRow
import dev.forcetower.melon.core.database.query.DisciplineDetailMaterialRow
import dev.forcetower.melon.core.database.query.EnrolledDisciplineRow
import dev.forcetower.melon.core.database.query.PartialGradeRow
import dev.forcetower.melon.core.database.query.RecentLectureRow
import dev.forcetower.melon.core.database.query.SemesterAllocationRow
import dev.forcetower.melon.core.database.query.SemesterClassAggregate
import dev.forcetower.melon.core.database.query.SemesterHoursProgressRow
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

    // One row per DisciplineOffer in the semester the student is enrolled in.
    // Aggregation is at the offer level so disciplines with multiple groups
    // (theory + practice) collapse to a single card. `weightedAverage` is
    // SUM(value × weight) / SUM(weight) across graded rows from every class
    // the student has in the offer — null when no grade has landed.
    // `finalGrade` / `approved` are the first non-null value across the
    // offer's StudentClass rows (the discipline's main group typically
    // carries both).
    @Query(
        """
        SELECT d.id AS disciplineId,
               o.id AS offerId,
               d.code AS code,
               d.name AS name,
               (
                 SELECT sc.finalGrade
                   FROM StudentClass sc
                   JOIN Class c ON c.id = sc.classId
                  WHERE c.offerId = o.id
                    AND sc.finalGrade IS NOT NULL
                    AND sc.finalGrade != ''
                  LIMIT 1
               ) AS finalGrade,
               (
                 SELECT sc.approved
                   FROM StudentClass sc
                   JOIN Class c ON c.id = sc.classId
                  WHERE c.offerId = o.id
                    AND sc.approved IS NOT NULL
                  LIMIT 1
               ) AS approved,
               (
                 SELECT SUM(CAST(sg.value AS REAL) * CAST(sg.weight AS REAL))
                        / NULLIF(SUM(CAST(sg.weight AS REAL)), 0)
                   FROM StudentGrade sg
                   JOIN StudentClass sc ON sc.id = sg.studentClassId
                   JOIN Class c ON c.id = sc.classId
                  WHERE c.offerId = o.id
                    AND sg.value IS NOT NULL
                    AND sg.value != ''
               ) AS weightedAverage
          FROM DisciplineOffer o
          JOIN Discipline d ON d.id = o.disciplineId
         WHERE o.semesterId = :semesterId
           AND EXISTS (
             SELECT 1
               FROM StudentClass sc
               JOIN Class c ON c.id = sc.classId
              WHERE c.offerId = o.id
           )
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

    // Class-hours elapsed vs. total for the semester. Elapsed is lecture-count
    // weighted per class: `c.hours * (lecturesBefore:today / lecturesTotal)`.
    // Classes with no lectures recorded contribute zero to `completedHours`,
    // matching the "nothing's been taught yet" reality — UI falls back to a
    // plain `0 / total` rather than inventing proportional hours.
    @Query(
        """
        SELECT COALESCE(SUM(c.hours), 0) AS totalHours,
               COALESCE(SUM(
                 CAST(c.hours AS REAL) * (
                   SELECT COUNT(*) FROM ClassLecture cl
                    WHERE cl.classId = c.id
                      AND cl.date IS NOT NULL
                      AND cl.date <= :today
                 ) * 1.0 / NULLIF((
                   SELECT COUNT(*) FROM ClassLecture cl2
                    WHERE cl2.classId = c.id
                      AND cl2.date IS NOT NULL
                 ), 0)
               ), 0.0) AS completedHours
          FROM Class c
          JOIN DisciplineOffer o ON o.id = c.offerId
         WHERE o.semesterId = :semesterId
        """,
    )
    abstract fun observeSemesterHoursProgress(
        semesterId: String,
        today: String,
    ): Flow<SemesterHoursProgressRow>

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
               sg.platformId AS gradePlatformId,
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

    // Every StudentClass row bound to a single DisciplineOffer, joined with its
    // class/offer/discipline and primary teacher. The detail-view use case
    // groups these by classId to produce one "group" (theory / practice) per
    // row. `disciplineProgram` carries the ementa — upstream's `ementa` field
    // is mapped into `Discipline.program` by the TS mappers, so reading
    // `d.program` here is the syllabus source of truth.
    @Query(
        """
        SELECT o.id AS offerId,
               o.semesterId AS semesterId,
               o.hours AS offerHours,
               d.id AS disciplineId,
               d.code AS disciplineCode,
               d.name AS disciplineName,
               d.hours AS disciplineHours,
               d.program AS disciplineProgram,
               d.department AS department,
               sc.id AS studentClassId,
               c.id AS classId,
               c.type AS classType,
               c.groupName AS groupName,
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
         WHERE o.id = :offerId
         ORDER BY c.type ASC, c.groupName ASC
        """,
    )
    abstract fun observeDisciplineOfferEnrollments(
        offerId: String,
    ): Flow<List<DisciplineDetailEnrollmentRow>>

    // Per-evaluation grade rows scoped to the offer, carrying evaluation name +
    // position and the upstream `gradePlatformId`. `gradePlatformId` is stable
    // across the per-class replication the backend does in `applyDiscipline` —
    // the use case uses it to dedup grades back to the upstream's single
    // shared set for multi-group disciplines.
    @Query(
        """
        SELECT sg.id AS gradeId,
               sg.platformId AS gradePlatformId,
               sg.studentClassId AS studentClassId,
               c.id AS classId,
               e.id AS evaluationId,
               e.name AS evaluationName,
               e.position AS evaluationPosition,
               sg.name AS gradeName,
               sg.nameShort AS gradeNameShort,
               sg.ordinal AS ordinal,
               sg.weight AS weight,
               sg.value AS value,
               sg.date AS date
          FROM StudentGrade sg
          JOIN ClassEvaluation e ON e.id = sg.evaluationId
          JOIN StudentClass sc ON sc.id = sg.studentClassId
          JOIN Class c ON c.id = sc.classId
          JOIN DisciplineOffer o ON o.id = c.offerId
         WHERE o.id = :offerId
         ORDER BY c.id ASC, e.position ASC, sg.ordinal ASC
        """,
    )
    abstract fun observeDisciplineOfferGrades(
        offerId: String,
    ): Flow<List<DisciplineDetailGradeRow>>

    // ClassLecture rows for every class under the offer. Attachment count uses
    // a correlated subquery so the result stays one-row-per-lecture; joining
    // against LectureMaterial directly would duplicate.
    @Query(
        """
        SELECT cl.id AS lectureId,
               c.id AS classId,
               cl.ordinal AS ordinal,
               cl.situation AS situation,
               cl.date AS date,
               cl.subject AS subject,
               (
                 SELECT COUNT(*) FROM LectureMaterial lm
                  WHERE lm.lectureId = cl.id
               ) AS attachmentCount
          FROM ClassLecture cl
          JOIN Class c ON c.id = cl.classId
          JOIN DisciplineOffer o ON o.id = c.offerId
         WHERE o.id = :offerId
         ORDER BY cl.date ASC, cl.ordinal ASC
        """,
    )
    abstract fun observeDisciplineOfferLectures(
        offerId: String,
    ): Flow<List<DisciplineDetailLectureRow>>

    // LectureMaterial rows for the offer, carrying the enclosing classId + the
    // lecture's date so the UI can group by class and label each with "added
    // on". Ordered newest-first to match the attachments block's layout.
    @Query(
        """
        SELECT lm.id AS materialId,
               lm.lectureId AS lectureId,
               c.id AS classId,
               cl.date AS lectureDate,
               lm.description AS caption,
               lm.url AS url
          FROM LectureMaterial lm
          JOIN ClassLecture cl ON cl.id = lm.lectureId
          JOIN Class c ON c.id = cl.classId
          JOIN DisciplineOffer o ON o.id = c.offerId
         WHERE o.id = :offerId
         ORDER BY cl.date DESC, lm.position ASC
        """,
    )
    abstract fun observeDisciplineOfferMaterials(
        offerId: String,
    ): Flow<List<DisciplineDetailMaterialRow>>

    private companion object {
        const val ALLOCATIONS_SQL = """
            SELECT a.id AS allocationId,
                   c.id AS classId,
                   o.id AS offerId,
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
