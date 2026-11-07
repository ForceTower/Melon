package dev.forcetower.unes.review

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.unes.remote.FeatureFlags
import dev.forcetower.unes.remote.FeatureGates
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class ReviewPrompterTest {
    private var state = ReviewState(activeDays = 5)
    private val store = mockk<ReviewPreferenceStore> {
        coEvery { read() } answers { state }
        coEvery { recordPrompt(any()) } answers { state = state.copy(lastPromptedAtMs = firstArg()) }
        coEvery { recordTrouble(any()) } answers { state = state.copy(lastTroubleAtMs = firstArg()) }
        coEvery { markCelebrated(any()) } returns true
    }
    private val reviewManager = mockk<ReviewManager> {
        every { requestReviewFlow() } returns completed(mockk<ReviewInfo>())
        every { launchReviewFlow(any(), any()) } returns completed(null)
    }
    private val analytics = mockk<Analytics>(relaxed = true)
    private val featureFlags = mockk<FeatureFlags> {
        every { gates } returns MutableStateFlow(FeatureGates(inAppReview = true))
    }
    private val activity = mockk<Activity>()

    @Test
    fun anEligibleMomentIsPresentedOnce() =
        runTest {
            val prompter = prompter(backgroundScope)
            presentRequests(prompter)

            prompter.reportMaterialUseful()
            runCurrent()

            verify(exactly = 1) { reviewManager.requestReviewFlow() }
            verify(exactly = 1) { analytics.selectContent(ContentTypes.REVIEW_PROMPT, "material_useful", any()) }
        }

    @Test
    fun twoMomentsQueuedTogetherSpendOnlyOnePrompt() =
        runTest {
            val prompter = prompter(backgroundScope)
            presentRequests(prompter)

            prompter.noteGradePushLaunch()
            prompter.reportApprovedDiscipline("o1")
            prompter.reportGradesViewed()
            runCurrent()

            verify(exactly = 1) { reviewManager.requestReviewFlow() }
            verify(exactly = 1) { reviewManager.launchReviewFlow(activity, any()) }
            verify(exactly = 1) { analytics.selectContent(ContentTypes.REVIEW_PROMPT, any(), any()) }
        }

    @Test
    fun troubleAfterAMomentIsQueuedCancelsThePrompt() =
        runTest {
            val prompter = prompter(backgroundScope)

            prompter.reportMaterialUseful()
            runCurrent()
            prompter.noteTrouble("sync_failed")
            runCurrent()
            presentRequests(prompter)
            runCurrent()

            verify(exactly = 0) { reviewManager.requestReviewFlow() }
            verify(exactly = 0) { analytics.selectContent(any(), any(), any()) }
        }

    private fun prompter(scope: CoroutineScope) =
        ReviewPrompter(
            reviewManager = reviewManager,
            store = store,
            featureFlags = featureFlags,
            sessionStore = mockk(relaxed = true),
            analytics = analytics,
            scope = scope,
        )

    private fun TestScope.presentRequests(prompter: ReviewPrompter) {
        backgroundScope.launch {
            prompter.requests.collect { prompter.present(activity, it) }
        }
        runCurrent()
    }

    private fun <T> completed(value: T): Task<T> =
        mockk {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { result } returns value
        }
}
