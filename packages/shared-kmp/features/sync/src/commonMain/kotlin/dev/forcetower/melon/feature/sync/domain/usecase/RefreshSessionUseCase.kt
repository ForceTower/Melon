package dev.forcetower.melon.feature.sync.domain.usecase

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Session refresh fired on every entry into the authenticated shell — fresh
// launch, logout→login, and background → foreground. Profile + first-page
// messages are cheap single requests; profile is what unsticks the overview
// footer's "sincronizando" label by importing the server's
// lastSyncCompletedAt.
//
// Profile + messages errors are logged and swallowed so a hiccup in one
// subsystem doesn't cancel the others. Per-semester payload errors still
// short-circuit because a partial tree is worse than none.
@Inject
class RefreshSessionUseCase internal constructor(
    private val mirror: MirrorRepository,
    logger: Logger,
) {
    private val log = logger.withTag("RefreshSessionUseCase")

    suspend operator fun invoke(): Outcome<Unit, SyncError> {
        log.i { "refresh session start" }

        when (val result = mirror.syncProfile()) {
            is Outcome.Err -> log.w { "profile refresh failed (ignored) err=${result.error}" }
            is Outcome.Ok -> Unit
        }

        when (val result = mirror.syncMessages(since = null, cursor = null)) {
            is Outcome.Err -> log.w { "messages first-page refresh failed (ignored) err=${result.error}" }
            is Outcome.Ok -> log.d { "messages first-page ok applied=${result.value.appliedCount}" }
        }

        when (val result = mirror.syncCalendarEvents()) {
            is Outcome.Err -> log.w { "calendar refresh failed (ignored) err=${result.error}" }
            is Outcome.Ok -> log.d { "calendar refresh ok applied=${result.value}" }
        }

        val listOutcome = mirror.syncSemesterList()
        val summaries = when (listOutcome) {
            is Outcome.Err -> {
                log.w { "refresh aborted at semester list err=${listOutcome.error}" }
                return listOutcome
            }
            is Outcome.Ok -> listOutcome.value
        }

        // Same rule as the KMP dashboard and iOS: every semester whose
        // [startDate, endDate] contains today; between terms, the most recent
        // one. Lex compare of yyyy-MM-dd matches calendar order.
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val inRange = summaries.filter { it.startDate <= today && today <= it.endDate }
        val targets = inRange.ifEmpty { listOfNotNull(summaries.maxByOrNull { it.startDate }) }
        log.i { "refresh semesters total=${summaries.size} targets=${targets.size}" }

        for (summary in targets) {
            when (val result = mirror.syncSemester(summary.id)) {
                is Outcome.Err -> {
                    log.w { "refresh failed on semester id=${summary.id} err=${result.error}" }
                    return result
                }
                is Outcome.Ok -> Unit
            }
        }

        log.i { "refresh complete semesters=${targets.size}" }
        return Outcome.Ok(Unit)
    }
}
