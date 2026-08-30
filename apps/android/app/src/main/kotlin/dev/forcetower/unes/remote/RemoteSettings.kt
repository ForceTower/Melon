package dev.forcetower.unes.remote

import dev.forcetower.lever.LeverClient
import dev.forcetower.lever.LeverKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Our remote config (https://github.com/ForceTower/lever), and the only source
// every gate resolves against.
//
// Reads are synchronous and total: a key lever has not published resolves to
// the type's empty value (`false` / `""`), which is the floor every gate is
// written against — a feature stays off and the captcha pair stays empty until
// something says otherwise. The client loads its cache when it is constructed
// and keeps the last activated values on disk, so gates hold their state
// offline and across launches.
@Singleton
internal class RemoteSettings @Inject constructor(private val client: LeverClient) {
    private val boolKeys = RemoteBoolKey.entries.associateWith { LeverKey.boolean(it.key, false) }
    private val stringKeys = RemoteStringKey.entries.associateWith { LeverKey.string(it.key, "") }

    // Emits on every value-changing activation — the launch fetch, the polling
    // floor, and the SSE nudge that lands a console publish while the app is
    // open, all through one channel.
    val changes: Flow<Unit> = client.updates.map { }

    fun bool(key: RemoteBoolKey): Boolean = client.value(boolKeys.getValue(key))

    fun string(key: RemoteStringKey): String = client.value(stringKeys.getValue(key))
}

// Parameter names are the keys shared with iOS. Android and iOS resolve them
// from the same lever environment, so a gate that should differ between the two
// is a platform condition on the parameter — not a second key.
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
