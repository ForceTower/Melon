package dev.forcetower.melon.core.analytics

// With only a couple of generic events, the analysis lives in these property
// *values* — so a typo here would silently split a funnel. Keep the known
// values as constants rather than inline strings at call sites. These are also
// the cross-platform contract: iOS mirrors the same strings.

object Screens {
    const val OVERVIEW = "overview"
    const val DISCIPLINES = "disciplines"
    const val SCHEDULE = "schedule"
    const val MESSAGES = "messages"
    const val ME = "me"
    const val MATERIALS = "materials"
    const val PARADOXO = "paradoxo"
    const val ENROLLMENT = "enrollment"
    const val CAMPUS_EVENT = "campus_event"
}

object ContentTypes {
    const val HUB = "hub"
    const val MATERIAL = "material"
    const val PARADOXO_ENTITY = "paradoxo_entity"
}
