package dev.forcetower.melon.core.sync.data.repository

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.dao.StudentDao
import dev.forcetower.melon.core.database.dao.UserDao
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.core.sync.data.dto.ProfileResponse
import dev.forcetower.melon.core.sync.data.dto.SemesterListResponse
import dev.forcetower.melon.core.sync.data.dto.SemesterPayloadResponse
import dev.forcetower.melon.core.sync.data.mapper.toEntity
import dev.forcetower.melon.core.sync.data.network.MirrorApi
import dev.forcetower.melon.core.sync.domain.model.SemesterSummary
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class MirrorRepositoryImpl(
    private val api: MirrorApi,
    private val userDao: UserDao,
    private val studentDao: StudentDao,
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
) : MirrorRepository {

    override suspend fun syncProfile(): Outcome<Unit, SyncError> = callNetwork {
        val response = api.getProfile()
        when (val mapped = classifyResponse<ProfileResponse>(response)) {
            is Outcome.Err -> mapped
            is Outcome.Ok -> {
                val payload = mapped.value
                // Preserve existing user row via `updateProfile` to avoid
                // nulling fields session wrote (e.g., imageUrl from login) if
                // the server later carries a null. Profile is the authority
                // here though, so straight upsert is acceptable too.
                userDao.upsert(payload.user.toEntity())
                payload.course?.let { studentDao.upsertCourse(it.toEntity()) }
                studentDao.upsertStudent(payload.student.toEntity())
                Outcome.Ok(Unit)
            }
        }
    }

    override suspend fun syncSemesterList(): Outcome<List<SemesterSummary>, SyncError> = callNetwork {
        val response = api.getSemesters()
        when (val mapped = classifyResponse<SemesterListResponse>(response)) {
            is Outcome.Err -> mapped
            is Outcome.Ok -> {
                val items = mapped.value.semesters
                // Upsert the semester rows so the client has the list even
                // before fetching full payloads. Scoped subtree (disciplines,
                // classes, grades) lands on each per-semester fetch.
                semesterDao.upsertAll(items.map { it.toEntity() })
                semesterDao.deleteMissing(items.map { it.id })
                Outcome.Ok(
                    items.map {
                        SemesterSummary(
                            id = it.id,
                            code = it.code,
                            description = it.description,
                            startDate = it.startDate,
                            endDate = it.endDate,
                            track = it.track,
                            dirtyAt = it.dirtyAt,
                        )
                    },
                )
            }
        }
    }

    override suspend fun syncSemester(semesterId: String): Outcome<Unit, SyncError> = callNetwork {
        val response = api.getSemesterPayload(semesterId)
        when (val mapped = classifyResponse<SemesterPayloadResponse>(response)) {
            is Outcome.Err -> mapped
            is Outcome.Ok -> {
                val p = mapped.value
                academicDao.applySemesterPayload(
                    semesterId = semesterId,
                    semester = p.semester.toEntity(),
                    disciplines = p.disciplines.map { it.toEntity() },
                    teachers = p.teachers.map { it.toEntity() },
                    spaces = p.spaces.map { it.toEntity() },
                    offers = p.disciplineOffers.map { it.toEntity() },
                    classes = p.classes.map { it.toEntity() },
                    classTeachers = p.classTeachers.map { it.toEntity() },
                    allocations = p.allocations.map { it.toEntity() },
                    studentClasses = p.studentClasses.map { it.toEntity() },
                    evaluations = p.evaluations.map { it.toEntity() },
                    grades = p.studentGrades.map { it.toEntity() },
                    lectures = p.lectures.map { it.toEntity() },
                    lectureMaterials = p.lectureMaterials.map { it.toEntity() },
                )
                Outcome.Ok(Unit)
            }
        }
    }

    private suspend inline fun <T> callNetwork(
        block: suspend () -> Outcome<T, SyncError>,
    ): Outcome<T, SyncError> = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: SerializationException) {
        Outcome.Err(SyncError.Unexpected)
    } catch (_: Throwable) {
        Outcome.Err(SyncError.NoConnection)
    }

    private suspend inline fun <reified T> classifyResponse(response: HttpResponse): Outcome<T, SyncError> {
        val statusCode = response.status.value
        return when (statusCode) {
            in 200..299 -> {
                val envelope = response.body<ApiEnvelope<T>>()
                val payload = envelope.data
                if (payload != null) Outcome.Ok(payload) else Outcome.Err(SyncError.Unexpected)
            }
            401 -> Outcome.Err(SyncError.Unauthorized)
            404 -> Outcome.Err(SyncError.NotFound)
            in 500..599 -> {
                val envelope = runCatching { response.body<ApiEnvelope<T>>() }.getOrNull()
                Outcome.Err(SyncError.Server(envelope?.message))
            }
            else -> Outcome.Err(SyncError.Unexpected)
        }
    }
}
