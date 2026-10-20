package dev.forcetower.melon.feature.transit.data

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.storage.KeyValueStorage
import dev.forcetower.melon.feature.transit.data.network.TransitCommutePlanDTO
import dev.forcetower.melon.feature.transit.data.network.TransitService
import dev.forcetower.melon.feature.transit.data.network.toDomain
import dev.forcetower.melon.feature.transit.data.network.toDto
import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.TransitError
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

// The student's saved Meu trajeto. The server is the source of truth: `save`
// and `remove` only touch the local mirror after the server accepted them,
// with what it answered; `refresh` pulls another device's changes. The mirror
// is one JSON snapshot in `KeyValueStorage` (like the campus event), and
// `observe` replays it on subscription and after every write so Hoje, Eu and
// the setup steps all see the same plan. Requests run one at a time, in
// order, so an older answer can never overwrite a newer one.
@SingleIn(AppScope::class)
@Inject
class CommutePlanRepository internal constructor(
    private val service: TransitService,
    private val storage: KeyValueStorage,
    private val json: Json,
    logger: Logger,
) {
    private val log = logger.withTag("CommutePlanRepository")
    private val state = MutableStateFlow(CommutePlan())
    private val hydration = Mutex()
    private var hydrated = false
    private val operations = Mutex()

    fun observe(): Flow<CommutePlan> =
        flow {
            hydrate()
            emitAll(state)
        }

    suspend fun refresh(): Outcome<Unit, TransitError> {
        hydrate()
        return operations.withLock {
            when (val outcome = service.plan()) {
                is Outcome.Ok -> {
                    apply(outcome.value)
                    log.d { "refresh ok directions=${outcome.value.directions.size}" }
                    Outcome.Ok(Unit)
                }
                is Outcome.Err -> {
                    // Once per screen entry, and the mirror stays; not a bug.
                    log.i { "refresh failed err=${outcome.error}" }
                    Outcome.Err(outcome.error)
                }
            }
        }
    }

    suspend fun save(plan: CommutePlan): Outcome<CommutePlan, TransitError> {
        hydrate()
        return operations.withLock {
            when (val outcome = service.savePlan(plan)) {
                is Outcome.Ok -> {
                    apply(outcome.value)
                    log.i { "save ok directions=${outcome.value.directions.size}" }
                    outcome
                }
                is Outcome.Err -> {
                    log.w { "save failed err=${outcome.error}" }
                    outcome
                }
            }
        }
    }

    suspend fun remove(): Outcome<Unit, TransitError> {
        hydrate()
        return operations.withLock {
            when (val outcome = service.removePlan()) {
                is Outcome.Ok -> {
                    apply(outcome.value)
                    log.i { "remove ok" }
                    Outcome.Ok(Unit)
                }
                is Outcome.Err -> {
                    log.w { "remove failed err=${outcome.error}" }
                    Outcome.Err(outcome.error)
                }
            }
        }
    }

    // Logout wipe — the plan is the student's own, so the next account on
    // the device must not inherit it.
    suspend fun clear() {
        hydration.withLock { hydrated = true }
        state.value = CommutePlan()
        storage.remove(STORAGE_KEY)
    }

    private suspend fun hydrate() {
        hydration.withLock {
            if (hydrated) return
            hydrated = true
            val cached = storage.get(STORAGE_KEY) ?: return
            runCatching { json.decodeFromString<TransitCommutePlanDTO>(cached) }
                .getOrNull()
                ?.let { state.value = it.toDomain() }
        }
    }

    private suspend fun apply(plan: CommutePlan) {
        state.value = plan
        if (plan.isEmpty) {
            storage.remove(STORAGE_KEY)
        } else {
            storage.put(STORAGE_KEY, json.encodeToString(plan.toDto()))
        }
    }

    private companion object {
        const val STORAGE_KEY = "transit.commute_plan"
    }
}
