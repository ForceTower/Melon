package dev.forcetower.melon.core.analytics

// With only a couple of generic events, the analysis lives in these property
// *values* — so a typo here would silently split a funnel. Keep the known
// values as constants rather than inline strings at call sites. These are also
// the cross-platform contract: iOS mirrors the same strings.

object Screens {
    // Onboarding (outer nav)
    const val WELCOME = "welcome"
    const val INTRO = "intro"
    const val LOGIN = "login"
    const val SYNC = "sync"
    const val READY = "ready"
    const val FOLIO_RUNNER = "folio_runner"

    // Tab roots
    const val OVERVIEW = "overview"
    const val DISCIPLINES = "disciplines"
    const val SCHEDULE = "schedule"
    const val MESSAGES = "messages"
    const val ME = "me"

    // Pushed screens
    const val DISCIPLINE_DETAIL = "discipline_detail"
    const val MESSAGE_DETAIL = "message_detail"
    const val SETTINGS = "settings"
    const val PASSKEYS = "passkeys"
    const val CALENDAR = "calendar"
    const val FINAL_COUNTDOWN = "final_countdown"
    const val LICENSES = "licenses"

    const val MATERIALS = "materials"
    const val MATERIALS_DISCIPLINE = "materials_discipline"
    const val MATERIALS_DETAIL = "materials_detail"
    const val MATERIALS_SAVED = "materials_saved"

    const val PARADOXO = "paradoxo"
    const val PARADOXO_DISCIPLINE = "paradoxo_discipline"
    const val PARADOXO_TEACHER = "paradoxo_teacher"
    const val PARADOXO_EXPLORE = "paradoxo_explore"

    const val ENROLLMENT = "enrollment"
    const val ENROLLMENT_OFFERS = "enrollment_offers"
    const val ENROLLMENT_DISCIPLINE = "enrollment_discipline"
    const val ENROLLMENT_TIMETABLE = "enrollment_timetable"
    const val ENROLLMENT_REVIEW = "enrollment_review"
    const val ENROLLMENT_SUCCESS = "enrollment_success"

    const val CAMPUS_EVENT = "campus_event"
    const val CAMPUS_EVENT_ACTIVITY = "campus_event_activity"
    const val CAMPUS_EVENT_SPEAKERS = "campus_event_speakers"
    const val CAMPUS_EVENT_WORKSHOPS = "campus_event_workshops"
    const val CAMPUS_EVENT_VENUES = "campus_event_venues"
    const val CAMPUS_EVENT_ORGANIZATIONS = "campus_event_organizations"
}

object ContentTypes {
    const val HUB = "hub"
    const val CTA = "cta"
    const val TILE = "tile"
    const val SHORTCUT = "shortcut"

    const val DISCIPLINE = "discipline"
    const val SEMESTER = "semester"
    const val SCHEDULE_DAY = "schedule_day"
    const val CALENDAR_EVENT = "calendar_event"
    const val MESSAGE = "message"
    const val ACTIVITY = "activity"

    const val MATERIAL = "material"
    const val PARADOXO_ENTITY = "paradoxo_entity"
    const val PARADOXO_EXPLORE = "paradoxo_explore"
    const val OFFER = "offer"
    const val ENROLLMENT = "enrollment"

    const val DOCUMENT = "document"
    const val SETTING = "setting"
    const val PASSKEY = "passkey"
}
