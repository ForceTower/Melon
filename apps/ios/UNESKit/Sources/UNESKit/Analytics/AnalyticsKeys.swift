// With only a couple of generic events, the analysis lives in these property
// *values* — a typo here would silently split a funnel. Keep the known values
// as constants rather than inline strings at call sites. These mirror the
// Android contract in `packages/shared-kmp/.../analytics/AnalyticsKeys.kt`
// verbatim so the two platforms never drift; add new values there first.

enum Screens {
    // Onboarding (outer nav)
    static let welcome = "welcome"
    static let intro = "intro"
    static let login = "login"
    static let sync = "sync"
    static let ready = "ready"
    static let folioRunner = "folio_runner"

    // Tab roots
    static let overview = "overview"
    static let disciplines = "disciplines"
    static let schedule = "schedule"
    static let messages = "messages"
    static let me = "me"

    // Pushed screens
    static let disciplineDetail = "discipline_detail"
    static let messageDetail = "message_detail"
    static let settings = "settings"
    static let passkeys = "passkeys"
    static let calendar = "calendar"
    static let finalCountdown = "final_countdown"
    static let licenses = "licenses"

    static let materials = "materials"
    static let materialsDiscipline = "materials_discipline"
    static let materialsDetail = "materials_detail"
    static let materialsSaved = "materials_saved"

    static let retrospective = "retrospective"
    static let paradoxo = "paradoxo"
    static let paradoxoDiscipline = "paradoxo_discipline"
    static let paradoxoTeacher = "paradoxo_teacher"
    static let paradoxoExplore = "paradoxo_explore"

    static let enrollment = "enrollment"
    static let enrollmentOffers = "enrollment_offers"
    static let enrollmentDiscipline = "enrollment_discipline"
    static let enrollmentTimetable = "enrollment_timetable"
    static let enrollmentReview = "enrollment_review"
    static let enrollmentSuccess = "enrollment_success"

    static let campusEvent = "campus_event"
    static let campusEventActivity = "campus_event_activity"
    static let campusEventSpeakers = "campus_event_speakers"
    static let campusEventWorkshops = "campus_event_workshops"
    static let campusEventVenues = "campus_event_venues"
    static let campusEventOrganizations = "campus_event_organizations"
}

enum ContentTypes {
    static let hub = "hub"
    static let cta = "cta"
    static let tile = "tile"
    static let shortcut = "shortcut"

    static let discipline = "discipline"
    static let semester = "semester"
    static let scheduleDay = "schedule_day"
    static let calendarEvent = "calendar_event"
    static let message = "message"
    static let activity = "activity"

    static let material = "material"
    static let paradoxoEntity = "paradoxo_entity"
    static let paradoxoExplore = "paradoxo_explore"
    static let offer = "offer"
    static let enrollment = "enrollment"

    static let document = "document"
    static let setting = "setting"
    static let passkey = "passkey"
}
