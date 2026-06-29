package dev.forcetower.melon.feature.enrollment.data.repository

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.feature.enrollment.data.dto.EnrollmentDisciplineDto
import dev.forcetower.melon.feature.enrollment.data.dto.EnrollmentMeetingDto
import dev.forcetower.melon.feature.enrollment.data.dto.EnrollmentOffersResponse
import dev.forcetower.melon.feature.enrollment.data.dto.EnrollmentPrereqDto
import dev.forcetower.melon.feature.enrollment.data.dto.EnrollmentSectionDto
import dev.forcetower.melon.feature.enrollment.data.dto.EnrollmentSelectionDto
import dev.forcetower.melon.feature.enrollment.data.dto.EnrollmentSlotDto
import dev.forcetower.melon.feature.enrollment.data.dto.EnrollmentWindowDto
import dev.forcetower.melon.feature.enrollment.data.dto.EnrollmentWindowResponse
import dev.forcetower.melon.feature.enrollment.data.dto.SubmitEnrollmentRequest
import dev.forcetower.melon.feature.enrollment.data.network.EnrollmentApi
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentAvailability
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentDiscipline
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentMeeting
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentOffers
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentPrerequisite
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSection
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSelection
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentShift
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSlot
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindow
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindowState
import dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentError
import dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentRepository
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
internal class EnrollmentRepositoryImpl(
    private val api: EnrollmentApi,
    logger: Logger,
) : EnrollmentRepository {

    private val log = logger.withTag("EnrollmentRepositoryImpl")

    override suspend fun window(): Outcome<EnrollmentAvailability, EnrollmentError> = guard("window") {
        when (val outcome = classify<EnrollmentWindowResponse>(api.window())) {
            is Outcome.Ok -> Outcome.Ok(outcome.value.toDomain())
            is Outcome.Err -> outcome
        }
    }

    override suspend fun offers(): Outcome<EnrollmentOffers, EnrollmentError> = guard("offers") {
        when (val outcome = classify<EnrollmentOffersResponse>(api.offers())) {
            is Outcome.Ok -> Outcome.Ok(EnrollmentOffers(outcome.value.disciplines.map { it.toDomain() }))
            is Outcome.Err -> outcome
        }
    }

    override suspend fun submit(selections: List<EnrollmentSelection>): Outcome<Unit, EnrollmentError> = guard("submit") {
        val body = SubmitEnrollmentRequest(
            selections = selections.map {
                EnrollmentSelectionDto(sectionId = it.sectionId, allowsOther = it.allowsOther, waitlist = it.waitlist)
            },
        )
        classifyEmpty(api.submit(body))
    }

    // Wraps a network call: rethrows cancellation, maps a decode failure to
    // Unexpected and any other throwable (transport) to NoConnection. Mirrors the
    // sync/settings repositories so errors never cross the KMP boundary as throws.
    private suspend fun <T> guard(
        op: String,
        block: suspend () -> Outcome<T, EnrollmentError>,
    ): Outcome<T, EnrollmentError> =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (ex: SerializationException) {
            log.e(throwable = ex) { "$op serialization failure" }
            Outcome.Err(EnrollmentError.Unexpected)
        } catch (ex: Throwable) {
            log.w(throwable = ex) { "$op transport failure" }
            Outcome.Err(EnrollmentError.NoConnection)
        }

    private suspend inline fun <reified T> classify(response: HttpResponse): Outcome<T, EnrollmentError> {
        return when (val code = response.status.value) {
            in 200..299 -> {
                val payload = response.body<ApiEnvelope<T>>().data
                if (payload != null) Outcome.Ok(payload) else Outcome.Err(EnrollmentError.Unexpected)
            }
            401 -> Outcome.Err(EnrollmentError.Unauthorized)
            in 500..599 -> Outcome.Err(EnrollmentError.Server(messageOf(response)))
            else -> Outcome.Err(EnrollmentError.Unexpected)
        }
    }

    // Submit returns an empty `data: {}`, so there's nothing to decode on success.
    private suspend fun classifyEmpty(response: HttpResponse): Outcome<Unit, EnrollmentError> {
        return when (val code = response.status.value) {
            in 200..299 -> Outcome.Ok(Unit)
            401 -> Outcome.Err(EnrollmentError.Unauthorized)
            in 500..599 -> Outcome.Err(EnrollmentError.Server(messageOf(response)))
            else -> Outcome.Err(EnrollmentError.Unexpected)
        }
    }

    private suspend fun messageOf(response: HttpResponse): String? =
        runCatching { response.body<ApiEnvelope<Unit>>().message }.getOrNull()
}

private fun EnrollmentWindowResponse.toDomain(): EnrollmentAvailability =
    EnrollmentAvailability(available = available, window = window?.toDomain())

private fun EnrollmentWindowDto.toDomain(): EnrollmentWindow =
    EnrollmentWindow(
        semester = semester,
        state = parseState(state),
        startDate = startDate,
        endDate = endDate,
        minHours = minHours,
        maxHours = maxHours,
        useQueue = useQueue,
        courseId = courseId,
    )

private fun parseState(raw: String): EnrollmentWindowState = when (raw) {
    "OPEN" -> EnrollmentWindowState.Open
    "UPCOMING" -> EnrollmentWindowState.Upcoming
    "CLOSED" -> EnrollmentWindowState.Closed
    else -> EnrollmentWindowState.Unknown
}

private fun EnrollmentDisciplineDto.toDomain(): EnrollmentDiscipline =
    EnrollmentDiscipline(
        id = id,
        code = code,
        name = name,
        workload = workload,
        mandatory = mandatory,
        gradePeriod = gradePeriod,
        suggestion = suggestion,
        prerequisites = prereqs.map { it.toDomain() },
        sections = sections.map { it.toDomain() },
    )

private fun EnrollmentPrereqDto.toDomain(): EnrollmentPrerequisite =
    EnrollmentPrerequisite(code = code, name = name, met = met)

private fun EnrollmentSectionDto.toDomain(): EnrollmentSection =
    EnrollmentSection(
        id = id,
        label = label,
        coursePreferential = coursePreferential,
        suggestion = suggestion,
        vacancies = vacancies,
        proposalsCount = proposalsCount,
        allowsOtherDefault = allowsOtherDefault,
        waitlistCount = waitlistCount,
        selected = selected,
        meetings = meetings.map { it.toDomain() },
    )

private fun EnrollmentMeetingDto.toDomain(): EnrollmentMeeting =
    EnrollmentMeeting(
        kind = kind,
        shift = parseShift(shift),
        professors = professors,
        room = room,
        slots = slots.map { it.toDomain() },
    )

private fun parseShift(raw: String): EnrollmentShift = when (raw) {
    "MORNING" -> EnrollmentShift.Morning
    "AFTERNOON" -> EnrollmentShift.Afternoon
    "NIGHT" -> EnrollmentShift.Night
    else -> EnrollmentShift.Undefined
}

private fun EnrollmentSlotDto.toDomain(): EnrollmentSlot =
    EnrollmentSlot(day = day, start = start, end = end)
