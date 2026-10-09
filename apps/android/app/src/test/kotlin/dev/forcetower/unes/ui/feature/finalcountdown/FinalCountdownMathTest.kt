package dev.forcetower.unes.ui.feature.finalcountdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Same examples as iOS `FinalCountdownMathTests`: averages floor to a tenth,
// "needed" grades ceil to a tenth, as the university records them.
internal class FinalCountdownMathTest {
    @Test
    fun directPassWhenEverythingClearsSeven() {
        val verdict = FinalCountdownMath.verdict(listOf(row("8,5"), row("7,8"), row("9")), weighted = false)
        assertEquals(FCVerdictKind.Passed, verdict.kind)
        assertEquals(8.4, verdict.avg)
    }

    @Test
    fun truncationKeepsSixNinetyFiveOutOfDirectPass() {
        // Raw mean 6,983… floors to 6,9 — never rounds up past the cutoff.
        val verdict = FinalCountdownMath.verdict(listOf(row("6,95"), row("7"), row("7")), weighted = false)
        assertEquals(FCVerdictKind.Final, verdict.kind)
        assertEquals(6.9, verdict.avg)
        assertEquals(2.2, verdict.need)
    }

    @Test
    fun finalNeedRoundsUpFromTheFormula() {
        val verdict = FinalCountdownMath.verdict(listOf(row("5,5"), row("4"), row("6,2")), weighted = false)
        assertEquals(FCVerdictKind.Final, verdict.kind)
        assertEquals(5.2, verdict.avg)
        // (5 − 0,6·5,2) / 0,4 is exactly 4,7 — float noise must not lift it.
        assertEquals(4.7, verdict.need)
    }

    @Test
    fun cutoffsAreInclusive() {
        val directPass = FinalCountdownMath.verdict(listOf(row("7"), row("7")), weighted = false)
        assertEquals(FCVerdictKind.Passed, directPass.kind)

        val lowestFinal = FinalCountdownMath.verdict(listOf(row("3"), row("3")), weighted = false)
        assertEquals(FCVerdictKind.Final, lowestFinal.kind)
        assertEquals(8.0, lowestFinal.need)

        // 2,95 truncates to 2,9.
        val justBelow = FinalCountdownMath.verdict(listOf(row("2,9"), row("3")), weighted = false)
        assertEquals(FCVerdictKind.Failed, justBelow.kind)
    }

    @Test
    fun failsOutrightBelowTheFloor() {
        val verdict = FinalCountdownMath.verdict(listOf(row("2"), row("2,5"), row("2,8")), weighted = false)
        assertEquals(FCVerdictKind.Failed, verdict.kind)
        assertEquals(2.4, verdict.avg)
    }

    @Test
    fun borderlineSolvesTheSingleMissingRow() {
        val verdict = FinalCountdownMath.verdict(listOf(row("6,5"), row("5,2"), row()), weighted = false)
        assertEquals(FCVerdictKind.Borderline, verdict.kind)
        assertEquals(5.8, verdict.avg)
        assertEquals(9.3, verdict.wildcardNeeded)
    }

    @Test
    fun borderlineFinalWhenEvenTenFallsShort() {
        val verdict = FinalCountdownMath.verdict(listOf(row("5"), row("4"), row()), weighted = false)
        assertEquals(FCVerdictKind.BorderlineFinal, verdict.kind)
        assertEquals(12.0, verdict.wildcardNeeded)
    }

    @Test
    fun passesEarlyWhenTheWorstCaseAlreadyClears() {
        val verdict = FinalCountdownMath.verdict(
            listOf(row("10"), row("10"), row("10"), row()),
            weighted = false,
        )
        assertEquals(FCVerdictKind.Passed, verdict.kind)
        assertEquals(7.5, verdict.avg)
    }

    @Test
    fun onTrackWhenSeveralRowsAreStillOpen() {
        val verdict = FinalCountdownMath.verdict(listOf(row("8"), row(), row()), weighted = false)
        assertEquals(FCVerdictKind.OnTrack, verdict.kind)
        assertNull(verdict.wildcardNeeded)
        assertEquals(9.3, verdict.best)
        assertEquals(2.6, verdict.worst)
    }

    @Test
    fun failingTrackWhenTheBestCaseStaysUnderTheFloor() {
        val verdict = FinalCountdownMath.verdict(listOf(row("0"), row("0"), row("0"), row()), weighted = false)
        assertEquals(FCVerdictKind.FailingTrack, verdict.kind)
        assertEquals(2.5, verdict.best)
    }

    @Test
    fun weightedModeUsesTheWeights() {
        val verdict = FinalCountdownMath.verdict(listOf(row("8", weight = 2), row("5"), row()), weighted = true)
        assertEquals(FCVerdictKind.Borderline, verdict.kind)
        assertEquals(7.0, verdict.avg)
        // (7·4 − 8·2 − 5·1) / 1
        assertEquals(7.0, verdict.wildcardNeeded)
    }

    @Test
    fun emptyUntilSomethingIsFilled() {
        val verdict = FinalCountdownMath.verdict(listOf(row(), row()), weighted = false)
        assertEquals(FCVerdictKind.Empty, verdict.kind)
        assertNull(verdict.avg)
    }

    @Test
    fun scoreTextSanitizing() {
        assertEquals("8,5", FCRow.sanitizeScoreText("8.5"))
        assertEquals("7,55", FCRow.sanitizeScoreText("7,5,5"))
        assertEquals("", FCRow.sanitizeScoreText("abc"))
        assertEquals("10", FCRow.sanitizeScoreText("15"))
        assertEquals(9.75, row("9,75").score)
        assertNull(row().score)
    }

    private fun row(
        score: String = "",
        weight: Int = 1,
    ) = FCRow(label = "AV", scoreText = score, weight = weight)
}
