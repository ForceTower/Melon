package dev.forcetower.melon.feature.campusevent.domain.model

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Campus event — integration weeks and extraordinary course events. Mirrors
// iOS `CampusEvent.swift`; the whole payload arrives in one fetch so every
// screen (and offline access) works from it.

// Where the event sits relative to now. Drives the Home card, the welcome
// reveal and the hub hero.
enum class CampusEventPhase { Upcoming, Live, Ended }

// Where one activity sits relative to now.
enum class CampusEventActivityState { Upcoming, Live, Past }

// The activity taxonomy. Unknown server kinds land on `Other` so newer
// categories degrade to a generic activity instead of dropping rows.
enum class CampusEventCategory { Quest, Workshop, Lecture, Presentation, GroupDynamic, Other }

// Who an activity is aimed at. Events that don't split their audience send
// `everyone` for every activity, which hides the filter entirely.
enum class CampusEventAudience {
    Everyone, Freshmen, Veterans;

    // Whether an activity aimed at `audience` shows under this filter.
    fun includes(audience: CampusEventAudience): Boolean =
        this == Everyone || audience == Everyone || audience == this
}

data class CampusEventActivity(
    val id: String,
    val title: String,
    val details: String?,
    val category: CampusEventCategory,
    val audience: CampusEventAudience,
    // Link into `venues` when the room is a real venue; the display name
    // stands alone for ad-hoc rooms ("Google Meet").
    val venueId: String?,
    val venueName: String,
    // Links into `speakers`; `speakerNames` stays the display source so
    // ad-hoc hosts ("DAECOMP") need no speaker entry.
    val speakerIds: List<String>,
    val speakerNames: List<String>,
    val startsAt: Instant,
    val endsAt: Instant?,
    val requiresSignup: Boolean,
) {
    // Open-ended activities count as live for a default stretch so they
    // leave the "happening now" spot on their own.
    val effectiveEnd: Instant get() = endsAt ?: (startsAt + 90.minutes)

    fun state(now: Instant): CampusEventActivityState = when {
        now >= effectiveEnd -> CampusEventActivityState.Past
        now >= startsAt -> CampusEventActivityState.Live
        else -> CampusEventActivityState.Upcoming
    }
}

data class CampusEventSpeaker(
    val id: String,
    val name: String,
    val role: String?,
    val organization: String?,
    val bio: String?,
    // Server-authored badge copy ("Docente", "Convidada", …).
    val tag: String?,
)

data class CampusEventWorkshop(
    val id: String,
    val title: String,
    val details: String?,
    val audience: CampusEventAudience,
    val venueName: String?,
    val instructors: String?,
    val requiresSignup: Boolean,
    val slots: Int?,
)

data class CampusEventVenue(
    val id: String,
    val name: String,
    val shortName: String?,
    // One-line hint of what happens there ("Palestras e apresentações").
    val hint: String?,
    // Schematic-map position, 0–100 on both axes. Venues without
    // coordinates keep the map hidden and render list-only.
    val mapX: Double?,
    val mapY: Double?,
) {
    val displayShortName: String get() = shortName ?: name
}

data class CampusEventOrganization(
    val id: String,
    val name: String,
    val fullName: String?,
    // Server-authored badge copy ("Organizador", "Liga", …).
    val tag: String?,
    val details: String?,
)

// One calendar day of the schedule, derived from activity dates.
data class CampusEventDay(
    val date: LocalDate,
    val activities: List<CampusEventActivity>,
)

data class CampusEvent(
    val id: String,
    // Bumped by the server on every publish; the repository skips identical
    // revisions so observers only wake for real changes.
    val revision: Int,
    // Short display name ("SIECOMP").
    val name: String,
    // Edition marker rendered above the name ("XXXIII").
    val edition: String?,
    // Long descriptive line ("Semana de Integração dos Estudantes…").
    val tagline: String?,
    // Welcome-screen eyebrow ("UEFS · Eng. de Computação").
    val institution: String?,
    // Footer credit ("Uma realização do DAECOMP · Gestão Bittencourt").
    val credit: String?,
    // IANA identifier of the campus zone ("America/Bahia"). Days and times
    // render in it so the schedule doesn't shift for a traveling student.
    val timeZoneIdentifier: String?,
    val startsAt: Instant,
    val endsAt: Instant,
    val activities: List<CampusEventActivity>,
    val speakers: List<CampusEventSpeaker>,
    val workshops: List<CampusEventWorkshop>,
    val venues: List<CampusEventVenue>,
    val organizations: List<CampusEventOrganization>,
) {
    val timeZone: TimeZone
        get() = timeZoneIdentifier
            ?.let { id -> runCatching { TimeZone.of(id) }.getOrNull() }
            ?: TimeZone.currentSystemDefault()

    fun phase(now: Instant): CampusEventPhase = when {
        now >= endsAt -> CampusEventPhase.Ended
        now >= startsAt -> CampusEventPhase.Live
        else -> CampusEventPhase.Upcoming
    }

    // Activities grouped into calendar days (event zone), both levels
    // sorted by time.
    fun days(): List<CampusEventDay> {
        val zone = timeZone
        return activities
            .groupBy { it.startsAt.toLocalDateTime(zone).date }
            .map { (date, dayActivities) ->
                CampusEventDay(date = date, activities = dayActivities.sortedBy { it.startsAt })
            }
            .sortedBy { it.date }
    }

    // The day the schedule should open on: today while live, the first day
    // before the event, the last one after.
    fun currentDayDate(now: Instant): LocalDate? {
        val days = days()
        return when (phase(now)) {
            CampusEventPhase.Upcoming -> days.firstOrNull()?.date
            CampusEventPhase.Ended -> days.lastOrNull()?.date
            CampusEventPhase.Live -> {
                val today = now.toLocalDateTime(timeZone).date
                days.firstOrNull { it.date >= today }?.date ?: days.lastOrNull()?.date
            }
        }
    }

    // 1-based "Dia N de M" index while the event runs.
    fun dayNumber(now: Instant): Int {
        val today = now.toLocalDateTime(timeZone).date
        val index = days().indexOfLast { it.date <= today }
        return if (index < 0) 1 else index + 1
    }

    val dayCount: Int get() = days().size

    // Whether `instant` falls on `day` in the event zone.
    fun isOnDay(instant: Instant, day: LocalDate): Boolean =
        instant.toLocalDateTime(timeZone).date == day

    // The activity opening the event — the hero's "Abertura" block.
    val opener: CampusEventActivity? get() = activities.minByOrNull { it.startsAt }

    fun activities(day: LocalDate, filter: CampusEventAudience): List<CampusEventActivity> =
        days().firstOrNull { it.date == day }
            ?.activities
            ?.filter { filter.includes(it.audience) }
            .orEmpty()

    fun activityCount(day: LocalDate, filter: CampusEventAudience): Int =
        activities(day, filter).size

    // Whether the schedule splits by audience at all; a single-audience
    // event renders without the filter.
    val hasAudienceSplit: Boolean
        get() = activities.any { it.audience != CampusEventAudience.Everyone }

    fun liveActivityCount(filter: CampusEventAudience, now: Instant): Int =
        activities.count {
            filter.includes(it.audience) && it.state(now) == CampusEventActivityState.Live
        }

    // The activity the live hero spotlights: the one running now, else the
    // next to start, else the closing one. `second` flags a running one.
    fun liveOrNextActivity(
        filter: CampusEventAudience,
        now: Instant,
    ): Pair<CampusEventActivity, Boolean>? {
        val matching = activities
            .filter { filter.includes(it.audience) }
            .sortedBy { it.startsAt }
        matching.firstOrNull { it.state(now) == CampusEventActivityState.Live }
            ?.let { return it to true }
        matching.firstOrNull { now < it.startsAt }
            ?.let { return it to false }
        return matching.lastOrNull()?.let { it to false }
    }

    // Resolves one of an activity's hosts: by id when linked, by display
    // name otherwise (ad-hoc hosts have neither and return null).
    fun speakerFor(activity: CampusEventActivity, index: Int): CampusEventSpeaker? {
        if (index < activity.speakerIds.size) {
            speakers.firstOrNull { it.id == activity.speakerIds[index] }?.let { return it }
        }
        val name = activity.speakerNames.getOrNull(index) ?: return null
        return speakers.firstOrNull { it.name == name }
    }

    // Resolves an activity's venue: by id when linked, by display name
    // otherwise (ad-hoc rooms like "Google Meet" return null).
    fun venueFor(activity: CampusEventActivity): CampusEventVenue? {
        activity.venueId?.let { id ->
            venues.firstOrNull { it.id == id }?.let { return it }
        }
        return venues.firstOrNull { it.name == activity.venueName }
    }

    fun activityCount(venue: CampusEventVenue): Int =
        activities.count { it.venueName == venue.name }
}
