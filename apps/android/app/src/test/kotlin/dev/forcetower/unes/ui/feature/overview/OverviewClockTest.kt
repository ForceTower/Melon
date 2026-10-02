package dev.forcetower.unes.ui.feature.overview

import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

internal class OverviewClockTest {
    @Test
    fun greetingAndSemesterCountdownUseTheScenarioTimezone() {
        val state = OverviewUiState(
            clock = Instant.parse("2026-10-03T01:00:00Z"),
            timeZone = TimeZone.of("America/Bahia"),
            locale = Locale.forLanguageTag("pt-BR"),
            semesterEndIso = "2026-10-04",
        )
        assertEquals(GreetingKind.Evening, state.greetingKind)
        assertEquals(2, state.semesterDaysLeft)
        assertEquals("Sex, 2 out", state.dateEyebrow.replaceFirstChar { it.uppercaseChar() })
    }
}
