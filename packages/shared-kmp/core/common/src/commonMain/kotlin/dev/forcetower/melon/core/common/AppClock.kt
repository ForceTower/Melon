package dev.forcetower.melon.core.common

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class AppClock(
    private val clock: Clock = Clock.System,
    private val fixedTimeZone: TimeZone? = null,
) {
    val timeZone: TimeZone get() = fixedTimeZone ?: TimeZone.currentSystemDefault()
    fun now() = clock.now()
    fun localNow() = now().toLocalDateTime(timeZone)
}
