package dev.forcetower.unes.ui.feature.commute

import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import java.time.LocalTime

// Today's class span, in minutes into the day — what decides whether the
// hero asks about getting to campus or going home (iOS `CommuteDaySchedule`).
internal data class CommuteDaySchedule(
    val firstStartMinute: Int? = null,
) {
    // The leg the student needs next: getting in until the first class starts
    // (or noon on a day without classes), going home after that. A plan with
    // one leg always answers that leg.
    fun direction(
        plan: CommutePlan,
        now: LocalTime,
    ): TransitDirection? {
        val directions = plan.directions
        if (directions.size < 2) return directions.firstOrNull()
        val minute = now.hour * 60 + now.minute
        return if (minute < (firstStartMinute ?: NOON_MINUTE)) {
            TransitDirection.ToCampus
        } else {
            TransitDirection.FromCampus
        }
    }

    companion object {
        private const val NOON_MINUTE = 12 * 60

        // "08:00" → 480; anything unparsable is left out of the span.
        fun fromStartTimes(startTimes: List<String>): CommuteDaySchedule =
            CommuteDaySchedule(
                firstStartMinute = startTimes.mapNotNull { time ->
                    val parts = time.take(5).split(':')
                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                    hour * 60 + minute
                }.minOrNull(),
            )
    }
}
