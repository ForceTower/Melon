package dev.forcetower.unes.remote

import kotlinx.coroutines.flow.Flow

// A remote-config source that always answers. Reads are synchronous and total:
// a key nothing has published resolves to the type's empty value (`false` /
// `""`), which is the floor every gate is written against — a feature stays off
// and the captcha pair stays empty until something says otherwise.
//
// Only the *base* source implements this. Layers stacked on top of it report
// absence instead (see `LeverRemoteSettings`), so they can decline a key and
// let the layer below answer.
internal interface RemoteSettings {
    fun bool(key: RemoteBoolKey): Boolean

    fun string(key: RemoteStringKey): String

    // Emits whenever the serving values changed. No replay: the current values
    // are always readable, so a collector wants the *next* change, not the last.
    val changes: Flow<Unit>

    // Kicks off fetching. Called once, from `MelonApp.onCreate`.
    fun start()
}

// Parameter names are the un-prefixed keys shared with iOS. Android and iOS
// resolve them from the same lever environment, so a gate that should differ
// between the two is a platform condition on the parameter — not a second key.
internal enum class RemoteBoolKey(val key: String) {
    ENROLLMENT("enable_enrollment"),
    ENROLLMENT_CERTIFICATE("enable_enrollment_certificate"),
    ACADEMIC_HISTORY("enable_academic_history"),
    PARADOXO("enable_paradoxo"),
    MATERIALS("enable_materials"),
    LIBRARY("enable_library"),
    CAMPUS_EVENT("enable_campus_event"),
    EVALUATION_REMINDERS("enable_evaluation_reminders"),
    COURSE_PROGRESS("enable_course_progress"),

    IN_APP_REVIEW("enable_in_app_review"),
}

internal enum class RemoteStringKey(val key: String) {
    DOCUMENT_CAPTCHA_SITE_KEY("document_captcha_site_key"),
    DOCUMENT_CAPTCHA_BASE_URL("document_captcha_base_url"),

    // Comma-separated `ReviewTrigger.tag` allow-list; empty means all of them.
    IN_APP_REVIEW_TRIGGERS("in_app_review_triggers"),
}
