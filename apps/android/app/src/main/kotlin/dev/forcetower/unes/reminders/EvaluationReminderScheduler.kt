package dev.forcetower.unes.reminders

import android.content.Context
import co.touchlab.kermit.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveEvaluationRemindersUseCase
import dev.forcetower.unes.di.ApplicationScope
import dev.forcetower.unes.firebase.FeatureFlags
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Keeps the reminder snapshot and its alarm honest: collects the KMP
// upcoming-evaluation flow gated by the Remote Config flag and the device-
// local switch, writes the snapshot, and re-arms the next 20:00-eve alarm.
// A gate turning off (or the last evaluation being graded) writes an empty
// snapshot, which cancels the alarm. `start()` is called from `MelonApp`
// and is idempotent.
@Singleton
internal class EvaluationReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val observeReminders: ObserveEvaluationRemindersUseCase,
    private val preferences: EvaluationReminderPreferenceStore,
    private val featureFlags: FeatureFlags,
    @param:ApplicationScope private val scope: CoroutineScope,
    logger: Logger,
) {
    private val log = logger.withTag("EvaluationReminderScheduler")
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        log.i { "subscribing to reminder data flows" }
        job = scope.launch {
            combine(
                observeReminders(),
                preferences.enabled,
                featureFlags.gates.map { it.evaluationReminders }.distinctUntilChanged(),
            ) { reminders, enabled, gate ->
                if (enabled && gate) reminders else emptyList()
            }
                .distinctUntilChanged()
                .collect { reminders ->
                    val snapshot = EvaluationReminderSnapshot(
                        reminders = reminders.map {
                            EvaluationReminderSnapshot.Entry(
                                key = it.key,
                                label = it.label,
                                disciplineName = it.disciplineName,
                                dateIso = it.date,
                            )
                        },
                    )
                    runCatching { EvaluationReminderSnapshot.save(context, snapshot) }
                        .onFailure { log.w(it) { "reminder snapshot write failed" } }
                    EvaluationReminderAlarms.rearm(context)
                    log.d { "reminder snapshot published count=${snapshot.reminders.size}" }
                }
        }
    }
}
