package dev.forcetower.melon.core.sync.data.repository

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.CalendarEventDao
import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.dao.StudentDao
import dev.forcetower.melon.core.database.dao.UserDao
import dev.forcetower.melon.core.database.dao.UserSettingsDao
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.sync.data.dto.CalendarEventsResponse
import dev.forcetower.melon.core.sync.data.dto.MessagePageResponse
import dev.forcetower.melon.core.sync.data.dto.MyCredentialsResponse
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
    private val calendarEventDao: CalendarEventDao,
    private val userSettingsDao: UserSettingsDao,
    private val sessionStore: SessionStore,
    logger: Logger,
) : MirrorRepository {

    private val log = logger.withTag("MirrorRepositoryImpl")

    override suspend fun syncProfile(): Outcome<Unit, SyncError> = callNetwork("syncProfile") {
        val response = api.getProfile()
        when (val mapped = classifyResponse<ProfileResponse>(response, "syncProfile")) {
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
                userSettingsDao.upsert(payload.settings.toEntity(payload.user.id))
                log.i { "syncProfile ok userId=${payload.user.id} studentId=${payload.student.id}" }
                Outcome.Ok(Unit)
            }
        }
    }

    override suspend fun syncSemesterList(): Outcome<List<SemesterSummary>, SyncError> = callNetwork("syncSemesterList") {
        val response = api.getSemesters()
        when (val mapped = classifyResponse<SemesterListResponse>(response, "syncSemesterList")) {
            is Outcome.Err -> mapped
            is Outcome.Ok -> {
                val items = mapped.value.semesters
                // Upsert the semester rows so the client has the list even
                // before fetching full payloads. Scoped subtree (disciplines,
                // classes, grades) lands on each per-semester fetch.
                semesterDao.upsertAll(items.map { it.toEntity() })
                semesterDao.deleteMissing(items.map { it.id })
                log.i { "syncSemesterList ok count=${items.size}" }
                Outcome.Ok(
                    items.map {
                        SemesterSummary(
                            id = it.id,
                            code = it.code,
                            desc = it.description,
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

    override suspend fun syncSemester(semesterId: String): Outcome<Unit, SyncError> = callNetwork("syncSemester") {
        log.d { "syncSemester start id=$semesterId" }
        val response = api.getSemesterPayload(semesterId)
        when (val mapped = classifyResponse<SemesterPayloadResponse>(response, "syncSemester id=$semesterId")) {
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
                log.i {
                    "syncSemester ok id=$semesterId disciplines=${p.disciplines.size} " +
                        "classes=${p.classes.size} grades=${p.studentGrades.size}"
                }
                Outcome.Ok(Unit)
            }
        }
    }

    override suspend fun fetchOnboardingStatus(): Outcome<OnboardingStatus, SyncError> = callNetwork("fetchOnboardingStatus") {
        val response = api.getOnboardingStatus()
        when (val mapped = classifyResponse<OnboardingStatusResponse>(response, "fetchOnboardingStatus")) {
            is Outcome.Err -> mapped
            is Outcome.Ok -> Outcome.Ok(mapped.value.toDomain())
        }
    }

    override suspend fun syncMessages(
        since: String?,
        cursor: String?,
    ): Outcome<MessagePageResult, SyncError> = callNetwork("syncMessages") {
        log.d { "syncMessages request since=${since ?: "<nil>"} cursor=${cursor ?: "<nil>"}" }
        val response = api.getMessages(since, cursor)
        when (val mapped = classifyResponse<MessagePageResponse>(response, "syncMessages")) {
            is Outcome.Err -> mapped
            is Outcome.Ok -> {
                val page = mapped.value
                val scopesCount = page.messages.sumOf { it.scopes.size }
                val attachmentsCount = page.messages.sumOf { it.attachments.size }
                // Server-merged read/starred land on the Message row itself;
                // the local MessageState overlay is deliberately left alone so
                // sync never resurrects an unread dot (matches iOS
                // `MirrorStore.upsertMessages`).
                messageDao.applyMessagePage(
                    messages = page.messages.map { it.toEntity() },
                    scopes = page.messages.flatMap { msg -> msg.scopes.map { it.toEntity(msg.id) } },
                    attachments = page.messages.flatMap { msg ->
                        msg.attachments.map { it.toEntity(msg.id) }
                    },
                )
                log.i {
                    "syncMessages ok messages=${page.messages.size} scopes=$scopesCount " +
                        "attachments=$attachmentsCount nextCursor=${page.nextCursor ?: "<nil>"}"
                }
                Outcome.Ok(MessagePageResult(appliedCount = page.messages.size, nextCursor = page.nextCursor))
            }
        }
    }

    override suspend fun syncCalendarEvents(): Outcome<Int, SyncError> = callNetwork("syncCalendarEvents") {
        val response = api.getCalendarEvents()
        when (val mapped = classifyResponse<CalendarEventsResponse>(response, "syncCalendarEvents")) {
            is Outcome.Err -> mapped
            is Outcome.Ok -> {
                val events = mapped.value.events.map { it.toEntity() }
                // Server emits the canonical 90-day window each call; replace
                // wholesale so upstream deletes propagate. The DAO wraps this
                // in a transaction so observers never see an empty state.
                calendarEventDao.replaceAll(events)
                log.i { "syncCalendarEvents ok applied=${events.size}" }
                Outcome.Ok(events.size)
            }
        }
    }

    override suspend fun syncMyCredentials(): Outcome<Unit, SyncError> = callNetwork("syncMyCredentials") {
        val response = api.getMyCredentials()
        when (val mapped = classifyResponse<MyCredentialsResponse>(response, "syncMyCredentials")) {
            is Outcome.Err -> mapped
            is Outcome.Ok -> {
                val credentials = mapped.value.credentials
                if (credentials != null) {
                    sessionStore.updateUpstreamCredentials(credentials.username, credentials.password)
                    log.i { "syncMyCredentials ok stored=true" }
                } else {
                    // Server has nothing on file (e.g. user has never logged in
                    // with username + password). Nothing to mirror; not an error.
                    log.i { "syncMyCredentials ok stored=false (server returned null)" }
                }
                Outcome.Ok(Unit)
            }
        }
    }

    override suspend fun pingActivity(): Outcome<Unit, SyncError> = callNetwork("pingActivity") {
        val statusCode = api.ping().status.value
        when (statusCode) {
            in 200..299 -> Outcome.Ok(Unit)
            401 -> {
                log.w { "pingActivity unauthorized" }
                Outcome.Err(SyncError.Unauthorized)
            }
            in 500..599 -> {
                log.w { "pingActivity server $statusCode" }
                Outcome.Err(SyncError.Server(null))
            }
            else -> {
                log.w { "pingActivity unexpected status $statusCode" }
                Outcome.Err(SyncError.Unexpected)
            }
        }
    }

    private suspend inline fun <T> callNetwork(
        op: String,
        block: suspend () -> Outcome<T, SyncError>,
    ): Outcome<T, SyncError> = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (ex: SerializationException) {
        // Response envelope didn't match the expected DTO shape — most often a
        // backend field drift that wasn't mirrored on the KMP side. Surface
        // the concrete exception so we don't guess at the schema mismatch.
        log.e(throwable = ex) { "$op serialization failure" }
        Outcome.Err(SyncError.Unexpected)
    } catch (ex: Throwable) {
        // All other throwables land here as `NoConnection` — transport
        // failures, Ktor body-parse errors, etc. Log with the real cause so
        // failures are diagnosable from the Xcode console / logcat.
        log.w(throwable = ex) { "$op transport failure" }
        Outcome.Err(SyncError.NoConnection)
    }

    private suspend inline fun <reified T> classifyResponse(
        response: HttpResponse,
        op: String,
    ): Outcome<T, SyncError> {
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
                    log.w { "$op 2xx envelope had null data (message=${envelope.message})" }
                    Outcome.Err(SyncError.Unexpected)
                }
            }
            401 -> {
                log.w { "$op unauthorized" }
                Outcome.Err(SyncError.Unauthorized)
            }
            404 -> {
                log.w { "$op not found" }
                Outcome.Err(SyncError.NotFound)
            }
            in 500..599 -> {
                val envelope = runCatching { response.body<ApiEnvelope<T>>() }.getOrNull()
                log.w { "$op server $statusCode message=${envelope?.message ?: "<none>"}" }
                Outcome.Err(SyncError.Server(envelope?.message))
            }
            else -> {
                log.w { "$op unexpected status $statusCode" }
                Outcome.Err(SyncError.Unexpected)
            }
        }
    }
}
