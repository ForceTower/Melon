package dev.forcetower.unes.review

import android.app.Activity
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManager
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.unes.di.ApplicationScope
import dev.forcetower.unes.remote.FeatureFlags
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

// Play In-App Reviews. Play grants roughly one sheet per user per quota window
// and never reports whether it appeared, so choosing the moment is the whole
// feature: screens report what just happened, and everything that decides
// whether it is worth the one prompt lives here.
//
// Same activity-agnostic singleton shape as `InAppUpdater`: signals arrive from
// ViewModels, the sheet needs an activity, so an eligible trigger parks on a
// conflated channel until `MainActivity` is resumed. Every failure path is
// silent — without Play (sideloads, emulators, `.debug`) this is a no-op.
@Singleton
internal class ReviewPrompter @Inject constructor(
    private val reviewManager: ReviewManager,
    private val store: ReviewPreferenceStore,
    private val featureFlags: FeatureFlags,
    private val sessionStore: SessionStore,
    private val analytics: Analytics,
    @ApplicationScope private val scope: CoroutineScope,
) {
    // Conflated: only one sheet can ever show, so a newer moment replaces an
    // undelivered older one rather than queueing behind it.
    private val pending = Channel<ReviewTrigger>(Channel.CONFLATED)

    val requests: Flow<ReviewTrigger> = pending.receiveAsFlow()

    @Volatile
    private var gradePushLaunchedAtMs = 0L
    private val paradoxoEntities = mutableSetOf<String>()

    fun start() {
        scope.launch {
            combine(
                sessionStore.sessionInvalid,
                sessionStore.credentialsInvalid,
            ) { session, credentials -> session || credentials }
                .collect { broken -> if (broken) noteTrouble("auth") }
        }
    }

    // Anything that made the app look broken mutes every trigger for a week —
    // the student would be rating the failure, not the app.
    fun noteTrouble(reason: String) {
        Timber.tag(TAG).d("review muted by trouble reason=%s", reason)
        scope.launch { store.recordTrouble(System.currentTimeMillis()) }
    }

    fun noteActiveDay() {
        scope.launch { store.noteActiveDay(LocalDate.now().toEpochDay()) }
    }

    fun noteGradePushLaunch() {
        gradePushLaunchedAtMs = System.currentTimeMillis()
    }

    // Only counts while the grade push that sent them there is still recent —
    // otherwise it's browsing, and browsing is not news.
    fun reportGradesViewed() {
        val since = System.currentTimeMillis() - gradePushLaunchedAtMs
        if (gradePushLaunchedAtMs == 0L || since > GRADE_PUSH_WINDOW_MS) return
        report(ReviewTrigger.GradeFromPush)
    }

    fun reportApprovedDiscipline(offerId: String) {
        scope.launch {
            if (store.markCelebrated(offerId)) evaluate(ReviewTrigger.PositiveVerdict)
        }
    }

    fun reportFailedDiscipline() = noteTrouble("failed_discipline")

    fun reportFinalCountdownPass() = report(ReviewTrigger.PositiveVerdict)

    fun reportMaterialUseful() = report(ReviewTrigger.MaterialUseful)

    // Depth, not arrival: one entity is a tap from the hub, two is exploring.
    fun reportParadoxoEntity(id: String) {
        val depth = synchronized(paradoxoEntities) {
            paradoxoEntities.add(id)
            paradoxoEntities.size
        }
        if (depth >= PARADOXO_DEPTH) report(ReviewTrigger.ParadoxoDepth)
    }

    suspend fun present(activity: Activity, trigger: ReviewTrigger) {
        val info = try {
            reviewManager.requestReview()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "review flow unavailable")
            return
        }
        // Recorded before launching and never revisited: Play reports nothing
        // about whether the sheet appeared, so the only honest accounting is
        // that the shot was spent.
        store.recordPrompt(System.currentTimeMillis())
        analytics.selectContent(ContentTypes.REVIEW_PROMPT, trigger.tag)
        Timber.tag(TAG).i("review flow launched trigger=%s", trigger.tag)
        try {
            reviewManager.launchReview(activity, info)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "review flow failed to launch")
        }
    }

    private fun report(trigger: ReviewTrigger) {
        scope.launch { evaluate(trigger) }
    }

    private suspend fun evaluate(trigger: ReviewTrigger) {
        val gates = featureFlags.gates.value
        val eligible = shouldRequestReview(
            trigger = trigger,
            state = store.read(),
            now = System.currentTimeMillis(),
            enabled = gates.inAppReview,
            allowedTriggers = parseReviewTriggers(gates.inAppReviewTriggers),
        )
        Timber.tag(TAG).d("review trigger=%s eligible=%b", trigger.tag, eligible)
        if (eligible) pending.trySend(trigger)
    }

    private companion object {
        const val TAG = "ReviewPrompter"
        val GRADE_PUSH_WINDOW_MS = 10.minutes.inWholeMilliseconds
        const val PARADOXO_DEPTH = 2
    }
}
