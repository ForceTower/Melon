package dev.forcetower.unes.ui.feature.onboarding.sync

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus
import dev.forcetower.melon.core.sync.domain.model.SemesterSummary
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.feature.sync.domain.usecase.FetchOnboardingStatusUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.PingActivityUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncMessagesUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncProfileUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncSemesterListUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncSemesterUseCase
import dev.forcetower.unes.di.ApplicationScope
import dev.forcetower.unes.firebase.PushRegistrar
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber

private data class SyncStep(
    val key: String,
    val minDurationMs: Long,
    val maxDurationMs: Long,
)

// Per-step durations mirror iOS `SYNC_STEPS` (apps/ios/.../SyncViewModel.swift).
// Phase 1 can take a while for alumni users — generous budgets stage the wait
// visibly rather than bailing. The visible order follows the dc onboarding
// spec (horário before turmas); every task runs concurrently regardless.
private val SYNC_STEPS = listOf(
    SyncStep(key = "auth", minDurationMs = 1_200, maxDurationMs = 3_000),
    SyncStep(key = "profile", minDurationMs = 800, maxDurationMs = 4_000),
    SyncStep(key = "schedule", minDurationMs = 800, maxDurationMs = 30_000),
    SyncStep(key = "classes", minDurationMs = 800, maxDurationMs = 30_000),
    SyncStep(key = "grades", minDurationMs = 800, maxDurationMs = 60_000),
    SyncStep(key = "msgs", minDurationMs = 800, maxDurationMs = 90_000),
)

private sealed interface StepResult {
    data object Ok : StepResult
    data class Fail(val authBroken: Boolean) : StepResult
}

data class SyncUiState(
    val currentStepIdx: Int = 0,
    val doneKeys: Set<String> = emptySet(),
) : UiState

sealed interface SyncIntent : UiIntent

sealed interface SyncEffect : UiEffect {
    data object Done : SyncEffect
    data object AuthFailed : SyncEffect
}

@HiltViewModel
class SyncViewModel @Inject internal constructor(
    private val pingActivity: PingActivityUseCase,
    private val syncProfile: SyncProfileUseCase,
    private val syncSemesterList: SyncSemesterListUseCase,
    private val syncSemester: SyncSemesterUseCase,
    private val syncMessages: SyncMessagesUseCase,
    private val fetchOnboardingStatus: FetchOnboardingStatusUseCase,
    private val pushRegistrar: PushRegistrar,
    private val sessionStore: SessionStore,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : MviViewModel<SyncUiState, SyncIntent, SyncEffect>(SyncUiState()) {

    private var didStart = false
    private var anyAuthBroken = false
    private var summaries: List<SemesterSummary> = emptyList()

    private var authTask: Deferred<StepResult>? = null
    private var profileTask: Deferred<StepResult>? = null
    private var classesTask: Deferred<StepResult>? = null
    private var scheduleTask: Deferred<StepResult>? = null
    private var gradesTask: Deferred<StepResult>? = null
    private var msgsTask: Deferred<StepResult>? = null

    init {
        start()
    }

    override fun onIntent(intent: SyncIntent) = Unit

    private fun start() {
        if (didStart) return
        didStart = true
        Timber.tag(TAG).i("sync: start")
        kickOffWork()
        viewModelScope.launch { driveAnimation() }
    }

    private fun kickOffWork() {
        authTask = viewModelScope.async { runAuthStep() }
        profileTask = viewModelScope.async { runProfileStep() }
        classesTask = viewModelScope.async { runClassesStep() }
        scheduleTask = viewModelScope.async { runScheduleStep() }
        gradesTask = viewModelScope.async { runGradesStep() }
        msgsTask = viewModelScope.async { runMessagesStep() }
    }

    // MARK: auth

    private suspend fun runAuthStep(): StepResult {
        runCatching { recordAuthIfFailed(pingActivity()) }
            .onFailure { Timber.tag(TAG).w(it, "ping failed") }
        // The FID usually lands before login and sits cached, unregistered —
        // reconcile now that the session exists.
        pushRegistrar.reconcile()
        return StepResult.Ok
    }

    // MARK: profile

    private suspend fun runProfileStep(): StepResult {
        repeat(3) { attempt ->
            try {
                when (val result = syncProfile()) {
                    is Outcome.Ok -> {
                        if (attempt == 2) return StepResult.Ok
                        if (!shouldRetryProfileForCourse()) return StepResult.Ok
                        delay(700)
                    }
                    is Outcome.Err -> {
                        val authBroken = result.error.isUnauthorized()
                        if (authBroken) anyAuthBroken = true
                        return StepResult.Fail(authBroken)
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                Timber.tag(TAG).w(t, "profile fetch threw")
                return StepResult.Fail(false)
            }
        }
        return StepResult.Ok
    }

    private suspend fun shouldRetryProfileForCourse(): Boolean {
        return runCatching {
            when (val result = fetchOnboardingStatus()) {
                is Outcome.Ok -> {
                    val status = result.value
                    val state = status.semesters.state
                    val backfilling = state == OnboardingStatus.PhaseStatus.State.Pending ||
                        state == OnboardingStatus.PhaseStatus.State.Running
                    !status.courseLinked && backfilling
                }
                is Outcome.Err -> false
            }
        }.getOrDefault(false)
    }

    // MARK: classes

    private suspend fun runClassesStep(): StepResult {
        val polled = pollUntilInitialTerminal(
            stepKey = "classes",
            maxTicks = 20,
            tickIntervalMs = 1_500,
        )
        if (polled is StepResult.Fail && polled.authBroken) return polled
        return fetchSemesterListAndStore()
    }

    // Returns Ok on terminal or budget exhaustion, Fail(true) on auth failure.
    private suspend fun pollUntilInitialTerminal(
        stepKey: String,
        maxTicks: Int,
        tickIntervalMs: Long,
    ): StepResult {
        repeat(maxTicks) { iter ->
            try {
                when (val result = fetchOnboardingStatus()) {
                    is Outcome.Ok -> {
                        val state = result.value.initial.state
                        val applied = result.value.initial.appliedSemesters
                        Timber.tag(TAG).i("$stepKey: iter=$iter initial=$state applied=$applied")
                        if (state == OnboardingStatus.PhaseStatus.State.Done ||
                            state == OnboardingStatus.PhaseStatus.State.Failed
                        ) {
                            return StepResult.Ok
                        }
                    }
                    is Outcome.Err -> {
                        val authBroken = result.error.isUnauthorized()
                        Timber.tag(TAG)
                            .w("$stepKey: status err iter=$iter authBroken=$authBroken err=${result.error}")
                        if (authBroken) {
                            anyAuthBroken = true
                            return StepResult.Fail(true)
                        }
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                Timber.tag(TAG).w(t, "$stepKey: status threw")
            }
            delay(tickIntervalMs)
        }
        Timber.tag(TAG).i("$stepKey: $maxTicks ticks exhausted without phase 1 terminal")
        return StepResult.Ok
    }

    private suspend fun fetchSemesterListAndStore(): StepResult {
        return try {
            when (val result = syncSemesterList()) {
                is Outcome.Ok -> {
                    summaries = result.value
                    Timber.tag(TAG).i("classes: list count=${result.value.size}")
                    StepResult.Ok
                }
                is Outcome.Err -> {
                    val authBroken = result.error.isUnauthorized()
                    if (authBroken) anyAuthBroken = true
                    StepResult.Fail(authBroken)
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "classes: list fetch threw")
            StepResult.Fail(false)
        }
    }

    // MARK: schedule

    private suspend fun runScheduleStep(): StepResult {
        val classesResult = classesTask?.await() ?: return StepResult.Ok
        if (classesResult is StepResult.Fail && classesResult.authBroken) {
            return StepResult.Fail(true)
        }

        if (summaries.isEmpty()) {
            val polled = pollUntilInitialTerminal(
                stepKey = "schedule",
                maxTicks = 20,
                tickIntervalMs = 1_500,
            )
            if (polled is StepResult.Fail && polled.authBroken) return polled
            fetchSemesterListAndStore()
        }

        val prioritized = pickActiveSemestersInOrder(summaries)
        val primary = prioritized.firstOrNull() ?: return StepResult.Ok

        val extras = prioritized.drop(1)
        if (extras.isNotEmpty()) {
            // Detached so they outlive the ViewModel — the umbrella graph owns
            // the network + DAOs they touch.
            val extraIds = extras.map { it.id }
            applicationScope.launch(Dispatchers.IO) {
                for (id in extraIds) {
                    runCatching { syncSemester(id) }
                }
            }
        }

        return try {
            when (val result = syncSemester(primary.id)) {
                is Outcome.Ok -> StepResult.Ok
                is Outcome.Err -> {
                    val authBroken = result.error.isUnauthorized()
                    if (authBroken) anyAuthBroken = true
                    StepResult.Fail(authBroken)
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "primary semester fetch threw")
            StepResult.Fail(false)
        }
    }

    // MARK: grades

    // Polls indefinitely on 1.5s intervals until the backend reports
    // appliedSemesters > 0 (or Phase 1 permanently failed). The only early
    // exit is initial.state == Failed — advancing onto empty data is worse
    // than a longer sync view.
    private suspend fun runGradesStep(): StepResult {
        var iter = 0
        while (true) {
            try {
                when (val result = fetchOnboardingStatus()) {
                    is Outcome.Ok -> {
                        val state = result.value.initial.state
                        val applied = result.value.initial.appliedSemesters
                        Timber.tag(TAG).i("grades: iter=$iter initial=$state applied=$applied")
                        if (applied > 0) return StepResult.Ok
                        if (state == OnboardingStatus.PhaseStatus.State.Failed) {
                            return StepResult.Ok
                        }
                    }
                    is Outcome.Err -> {
                        val authBroken = result.error.isUnauthorized()
                        if (authBroken) {
                            anyAuthBroken = true
                            return StepResult.Fail(true)
                        }
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                Timber.tag(TAG).w(t, "grades: status threw")
            }
            iter += 1
            delay(1_500)
        }
        @Suppress("UNREACHABLE_CODE")
        return StepResult.Ok
    }

    // MARK: messages

    private suspend fun runMessagesStep(): StepResult {
        val polled = pollUntilInitialTerminal(
            stepKey = "msgs",
            maxTicks = 60,
            tickIntervalMs = 1_500,
        )
        if (polled is StepResult.Fail && polled.authBroken) return polled

        return try {
            when (val result = syncMessages(since = null, cursor = null)) {
                is Outcome.Ok -> {
                    val nextCursor = result.value.nextCursor
                    Timber.tag(TAG).i(
                        "msgs: first page applied=${result.value.appliedCount} nextCursor=$nextCursor",
                    )
                    if (nextCursor != null) startBackgroundPagination(nextCursor)
                    StepResult.Ok
                }
                is Outcome.Err -> {
                    val authBroken = result.error.isUnauthorized()
                    if (authBroken) anyAuthBroken = true
                    StepResult.Fail(authBroken)
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "messages first page threw")
            StepResult.Fail(false)
        }
    }

    private fun startBackgroundPagination(firstCursor: String) {
        // Detached background pagination — doesn't block onDone.
        applicationScope.launch(Dispatchers.IO) {
            var cursor: String? = firstCursor
            var pages = 0
            val maxPages = 20
            while (cursor != null && pages < maxPages) {
                val outcome = runCatching { syncMessages(since = null, cursor = cursor) }
                    .getOrNull() ?: break
                when (outcome) {
                    is Outcome.Ok -> {
                        cursor = outcome.value.nextCursor
                        pages += 1
                    }
                    is Outcome.Err -> {
                        Timber.tag(TAG).w("msgs: background page ${pages + 1} err=${outcome.error}")
                        return@launch
                    }
                }
            }
            Timber.tag(TAG).i("msgs: background pagination done pages=$pages")
        }
    }

    // MARK: animation

    private suspend fun driveAnimation() {
        SYNC_STEPS.forEachIndexed { idx, step ->
            setState { copy(currentStepIdx = idx) }
            waitForStep(step)
            setState { copy(doneKeys = doneKeys + step.key) }
        }
        waitForReadiness()
        if (anyAuthBroken) {
            Timber.tag(TAG).i("sync: finish -> AuthFailed")
            runCatching { pushRegistrar.unregisterAll() }
            runCatching { sessionStore.logout() }
            emitEffect(SyncEffect.AuthFailed)
        } else {
            Timber.tag(TAG).i("sync: finish -> Done")
            emitEffect(SyncEffect.Done)
        }
    }

    private suspend fun waitForStep(step: SyncStep) {
        val task = task(step.key)
        val started = nowMs()
        if (task != null) {
            withTimeoutOrNull(step.maxDurationMs) { task.await() }
        }
        val elapsed = nowMs() - started
        if (elapsed >= step.maxDurationMs) {
            Timber.tag(TAG).i("anim: step=${step.key} TIMED_OUT after ${elapsed}ms")
        }
        val remaining = step.minDurationMs - elapsed
        if (remaining > 0) delay(remaining)
    }

    private suspend fun waitForReadiness() {
        Timber.tag(TAG).i("readiness: awaiting profile + schedule + grades + msgs")
        profileTask?.await()
        scheduleTask?.let { withTimeoutOrNull(2_000) { it.await() } }
        gradesTask?.await()
        msgsTask?.let { withTimeoutOrNull(3_000) { it.await() } }
        Timber.tag(TAG).i("readiness: done")
    }

    private fun task(key: String): Deferred<StepResult>? = when (key) {
        "auth" -> authTask
        "profile" -> profileTask
        "classes" -> classesTask
        "schedule" -> scheduleTask
        "grades" -> gradesTask
        "msgs" -> msgsTask
        else -> null
    }

    // MARK: helpers

    private fun pickActiveSemestersInOrder(all: List<SemesterSummary>): List<SemesterSummary> {
        if (all.isEmpty()) return emptyList()
        val sorted = all.sortedByDescending { it.startDate }
        val today = todayIsoDate()
        val active = sorted.filter { it.startDate <= today && today <= it.endDate }
        return active.ifEmpty { listOf(sorted.first()) }
    }

    private fun todayIsoDate(): String =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

    private fun nowMs(): Long = System.currentTimeMillis()

    private fun recordAuthIfFailed(outcome: Outcome<Unit, SyncError>) {
        if (outcome is Outcome.Err && outcome.error.isUnauthorized()) {
            anyAuthBroken = true
        }
    }

    private fun SyncError.isUnauthorized(): Boolean = this is SyncError.Unauthorized

    private companion object {
        const val TAG = "SyncViewModel"
    }
}
