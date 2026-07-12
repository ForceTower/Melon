package dev.forcetower.melon.feature.campusevent.data.network

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEvent
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventActivity
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventAudience
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventCategory
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventError
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventOrganization
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventSpeaker
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventVenue
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventWorkshop
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlin.time.Instant
import kotlinx.serialization.Serializable

// `GET api/campus-events/current` — the single featured event for the
// student, or `event: null` when nothing is featured. Mirrors iOS
// `CampusEventRepository+Live.swift`: unknown categories degrade to `Other`
// and unknown/absent audiences to `Everyone` so rows are never dropped.
@Inject
internal class CampusEventService(
    private val client: HttpClient,
) {
    // Ok(null) means "nothing featured" — distinct from a failed fetch,
    // which must keep any stale payload alive.
    suspend fun current(): Outcome<CampusEventDTO?, CampusEventError> {
        val payload = try {
            val response = client.get("api/campus-events/current")
            if (!response.status.isSuccess()) return Outcome.Err(CampusEventError.Connection)
            response.body<ApiEnvelope<CampusEventFeedDTO>>().takeIf { it.ok }?.data
        } catch (_: Exception) {
            null
        } ?: return Outcome.Err(CampusEventError.Connection)
        return Outcome.Ok(payload.event)
    }
}

@Serializable
internal data class CampusEventFeedDTO(
    val event: CampusEventDTO? = null,
)

@Serializable
internal data class CampusEventDTO(
    val id: String,
    val revision: Int? = null,
    val name: String,
    val edition: String? = null,
    val tagline: String? = null,
    val institution: String? = null,
    val credit: String? = null,
    val timezone: String? = null,
    val startsAt: String,
    val endsAt: String,
    val activities: List<ActivityDTO>,
    val speakers: List<SpeakerDTO>? = null,
    val workshops: List<WorkshopDTO>? = null,
    val venues: List<VenueDTO>? = null,
    val organizations: List<OrganizationDTO>? = null,
) {
    @Serializable
    internal data class ActivityDTO(
        val id: String,
        val title: String,
        val details: String? = null,
        val category: String,
        val audience: String? = null,
        val venueId: String? = null,
        val venueName: String,
        val speakerIds: List<String>? = null,
        val speakerNames: List<String>? = null,
        val startsAt: String,
        val endsAt: String? = null,
        val requiresSignup: Boolean? = null,
    )

    @Serializable
    internal data class SpeakerDTO(
        val id: String,
        val name: String,
        val role: String? = null,
        val organization: String? = null,
        val bio: String? = null,
        val tag: String? = null,
    )

    @Serializable
    internal data class WorkshopDTO(
        val id: String,
        val title: String,
        val details: String? = null,
        val audience: String? = null,
        val venueName: String? = null,
        val instructors: String? = null,
        val requiresSignup: Boolean? = null,
        val slots: Int? = null,
    )

    @Serializable
    internal data class VenueDTO(
        val id: String,
        val name: String,
        val shortName: String? = null,
        val hint: String? = null,
        val mapX: Double? = null,
        val mapY: Double? = null,
    )

    @Serializable
    internal data class OrganizationDTO(
        val id: String,
        val name: String,
        val fullName: String? = null,
        val tag: String? = null,
        val details: String? = null,
    )
}

// Null only when the event-level dates don't parse — a payload the clients
// can't render at all. Per-activity date failures drop just that activity.
internal fun CampusEventDTO.toDomainOrNull(): CampusEvent? {
    val start = parseInstant(startsAt) ?: return null
    val end = parseInstant(endsAt) ?: return null
    return CampusEvent(
        id = id,
        revision = revision ?: 1,
        name = name,
        edition = edition,
        tagline = tagline,
        institution = institution,
        credit = credit,
        timeZoneIdentifier = timezone,
        startsAt = start,
        endsAt = end,
        activities = activities.mapNotNull { it.toDomainOrNull() },
        speakers = speakers.orEmpty().map { it.toDomain() },
        workshops = workshops.orEmpty().map { it.toDomain() },
        venues = venues.orEmpty().map { it.toDomain() },
        organizations = organizations.orEmpty().map { it.toDomain() },
    )
}

private fun CampusEventDTO.ActivityDTO.toDomainOrNull(): CampusEventActivity? {
    val start = parseInstant(startsAt) ?: return null
    return CampusEventActivity(
        id = id,
        title = title,
        details = details,
        category = categoryOf(category),
        audience = audienceOf(audience),
        venueId = venueId,
        venueName = venueName,
        speakerIds = speakerIds.orEmpty(),
        speakerNames = speakerNames.orEmpty(),
        startsAt = start,
        endsAt = endsAt?.let(::parseInstant),
        requiresSignup = requiresSignup ?: false,
    )
}

private fun CampusEventDTO.SpeakerDTO.toDomain() = CampusEventSpeaker(
    id = id,
    name = name,
    role = role,
    organization = organization,
    bio = bio,
    tag = tag,
)

private fun CampusEventDTO.WorkshopDTO.toDomain() = CampusEventWorkshop(
    id = id,
    title = title,
    details = details,
    audience = audienceOf(audience),
    venueName = venueName,
    instructors = instructors,
    requiresSignup = requiresSignup ?: false,
    slots = slots,
)

private fun CampusEventDTO.VenueDTO.toDomain() = CampusEventVenue(
    id = id,
    name = name,
    shortName = shortName,
    hint = hint,
    mapX = mapX,
    mapY = mapY,
)

private fun CampusEventDTO.OrganizationDTO.toDomain() = CampusEventOrganization(
    id = id,
    name = name,
    fullName = fullName,
    tag = tag,
    details = details,
)

private fun categoryOf(raw: String): CampusEventCategory = when (raw) {
    "quest" -> CampusEventCategory.Quest
    "workshop" -> CampusEventCategory.Workshop
    "lecture" -> CampusEventCategory.Lecture
    "presentation" -> CampusEventCategory.Presentation
    "dynamic" -> CampusEventCategory.GroupDynamic
    else -> CampusEventCategory.Other
}

private fun audienceOf(raw: String?): CampusEventAudience = when (raw) {
    "freshmen" -> CampusEventAudience.Freshmen
    "veterans" -> CampusEventAudience.Veterans
    else -> CampusEventAudience.Everyone
}

private fun parseInstant(raw: String): Instant? =
    runCatching { Instant.parse(raw) }.getOrNull()
