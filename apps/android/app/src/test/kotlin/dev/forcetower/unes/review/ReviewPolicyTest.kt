package dev.forcetower.unes.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class ReviewPolicyTest {

    private val now = 1_800_000_000_000L

    @Test
    fun `a healthy user on an allowed trigger is asked`() {
        assertTrue(eligible())
    }

    @Test
    fun `the kill switch wins over everything`() {
        assertFalse(eligible(enabled = false))
    }

    @Test
    fun `a trigger outside the allow-list never fires`() {
        assertFalse(
            eligible(
                trigger = ReviewTrigger.ParadoxoDepth,
                allowed = setOf(ReviewTrigger.PositiveVerdict),
            ),
        )
    }

    @Test
    fun `a fresh install has not earned the question`() {
        assertFalse(eligible(state = healthy.copy(activeDays = 0)))
        assertFalse(eligible(state = healthy.copy(activeDays = 2)))
        assertTrue(eligible(state = healthy.copy(activeDays = 3)))
    }

    @Test
    fun `recent trouble mutes every trigger`() {
        assertFalse(eligible(state = healthy.copy(lastTroubleAtMs = now - 6.days.inWholeMilliseconds)))
        assertTrue(eligible(state = healthy.copy(lastTroubleAtMs = now - 8.days.inWholeMilliseconds)))
    }

    @Test
    fun `the cooldown holds until it expires`() {
        assertFalse(eligible(state = healthy.copy(lastPromptedAtMs = now - 44.days.inWholeMilliseconds)))
        assertTrue(eligible(state = healthy.copy(lastPromptedAtMs = now - 46.days.inWholeMilliseconds)))
    }

    @Test
    fun `a clock moved backwards suppresses rather than re-asks`() {
        assertFalse(eligible(state = healthy.copy(lastPromptedAtMs = now + 10.days.inWholeMilliseconds)))
    }

    @Test
    fun `a blank allow-list means every trigger`() {
        assertEquals(ReviewTrigger.entries.toSet(), parseReviewTriggers(""))
        assertEquals(ReviewTrigger.entries.toSet(), parseReviewTriggers("   "))
    }

    @Test
    fun `a published allow-list narrows to its known tags`() {
        assertEquals(
            setOf(ReviewTrigger.PositiveVerdict, ReviewTrigger.MaterialUseful),
            parseReviewTriggers("positive_verdict, material_useful, not_a_trigger"),
        )
    }

    private val healthy = ReviewState(activeDays = 5)

    private fun eligible(
        trigger: ReviewTrigger = ReviewTrigger.PositiveVerdict,
        state: ReviewState = healthy,
        enabled: Boolean = true,
        allowed: Set<ReviewTrigger> = ReviewTrigger.entries.toSet(),
    ) = shouldRequestReview(
        trigger = trigger,
        state = state,
        now = now,
        enabled = enabled,
        allowedTriggers = allowed,
    )
}
