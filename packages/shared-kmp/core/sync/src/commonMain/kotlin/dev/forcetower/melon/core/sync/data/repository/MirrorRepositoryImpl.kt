package dev.forcetower.melon.core.sync.data.repository

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.dao.StudentDao
import dev.forcetower.melon.core.database.dao.UserDao
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.core.sync.data.dto.MessagePageResponse
import dev.forcetower.melon.core.sync.data.dto.OnboardingStatusResponse
import dev.forcetower.melon.core.sync.data.dto.ProfileResponse
import dev.forcetower.melon.core.sync.data.dto.SemesterListResponse
import dev.forcetower.melon.core.sync.data.dto.SemesterPayloadResponse
import dev.forcetower.melon.core.sync.data.mapper.toDomain
import dev.forcetower.melon.core.sync.data.mapper.toEntity
import dev.forcetower.melon.core.sync.data.network.MirrorApi
import dev.forcetower.melon.core.sync.domain.model.MessagePageResult
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus
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
    private val messageDao: MessageDao,
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
                studentDao.upsertStudent(payload.student.toEntity(payload.lastSyncCompletedAt))
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

    override suspend fun fetchOnboardingStatus(): Outcome<OnboardingStatus, SyncError> = callNetwork {
        val response = api.getOnboardingStatus()
        when (val mapped = classifyResponse<OnboardingStatusResponse>(response)) {
            is Outcome.Err -> mapped
            is Outcome.Ok -> Outcome.Ok(mapped.value.toDomain())
        }
    }

    override suspend fun syncMessages(
        since: String?,
        cursor: String?,
    ): Outcome<MessagePageResult, SyncError> = callNetwork {
        val response = api.getMessages(since, cursor)
        when (val mapped = classifyResponse<MessagePageResponse>(response)) {
            is Outcome.Err -> mapped
            is Outcome.Ok -> {
                val page = mapped.value
                // Read/starred state is per-student locally but server-merged
                // across all linked students — without a 1:1 student mapping
                // we skip persisting MessageState here. Fresh inbox lands as
                // unread; a later state-sync pass can reconcile.
                messageDao.applyMessagePage(
                    messages = page.messages.map { it.toEntity() },
                    scopes = page.messages.flatMap { msg -> msg.scopes.map { it.toEntity(msg.id) } },
                    attachments = page.messages.flatMap { msg ->
                        msg.attachments.map { it.toEntity(msg.id) }
                    },
                )
                Outcome.Ok(MessagePageResult(appliedCount = page.messages.size, nextCursor = page.nextCursor))
            }
        }
    }

    override suspend fun pingActivity(): Outcome<Unit, SyncError> = callNetwork {
        val statusCode = api.ping().status.value
        when (statusCode) {
            in 200..299 -> Outcome.Ok(Unit)
            401 -> Outcome.Err(SyncError.Unauthorized)
            in 500..599 -> Outcome.Err(SyncError.Server(null))
            else -> Outcome.Err(SyncError.Unexpected)
        }
    }

    private suspend inline fun <T> callNetwork(
        block: suspend () -> Outcome<T, SyncError>,
    ): Outcome<T, SyncError> = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (ex: SerializationException) {
        // Response envelope didn't match the expected DTO shape — most often a
        // backend field drift that wasn't mirrored on the KMP side. Surface
        // the concrete exception so we don't guess at the schema mismatch.
        logSyncFailure("SerializationException", ex)
        Outcome.Err(SyncError.Unexpected)
    } catch (ex: Throwable) {
        // All other throwables land here as `NoConnection` — used to include
        // everything from transport failures to Ktor body-parse errors, with
        // no signal. Log the real cause (class + message) so failures are
        // diagnosable from the Xcode console / logcat.
        logSyncFailure(ex::class.simpleName ?: "Throwable", ex)
        Outcome.Err(SyncError.NoConnection)
    }

    private suspend inline fun <reified T> classifyResponse(response: HttpResponse): Outcome<T, SyncError> {
        val statusCode = response.status.value
        return when (statusCode) {
            in 200..299 -> {
                // Body-decode failures (schema drift, type mismatch, etc.)
                // propagate to `callNetwork`, which logs + classifies them —
                // SerializationException → Unexpected, everything else →
                // NoConnection. Handling this here would split the logging.
                val envelope = response.body<ApiEnvelope<T>>()
                val payload = envelope.data
                if (payload != null) {
                    Outcome.Ok(payload)
                } else {
                    logSyncIssue("2xx envelope had null `data` (message=${envelope.message})")
                    Outcome.Err(SyncError.Unexpected)
                }
            }
            401 -> Outcome.Err(SyncError.Unauthorized)
            404 -> Outcome.Err(SyncError.NotFound)
            in 500..599 -> {
                val envelope = runCatching { response.body<ApiEnvelope<T>>() }.getOrNull()
                logSyncIssue("server $statusCode: ${envelope?.message ?: "<no message>"}")
                Outcome.Err(SyncError.Server(envelope?.message))
            }
            else -> {
                logSyncIssue("unexpected status $statusCode")
                Outcome.Err(SyncError.Unexpected)
            }
        }
    }
}

// Routed to stdout so Xcode's console picks it up on iOS and logcat on
// Android. Keep tag + payload compact — this runs on every sync failure.
private fun logSyncFailure(kind: String, ex: Throwable) {
    println("[MirrorRepository] $kind: ${ex.message ?: "<no message>"}")
    ex.printStackTrace()
}

private fun logSyncIssue(message: String) {
    println("[MirrorRepository] $message")
}
