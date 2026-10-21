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

    // Guards the mirror: its hydration, every write, and the generation a
    // logout bumps. A request begun under the previous account neither goes
    // out on its turn (the bearer by then may be the next account's) nor
    // writes its answer into the next one's mirror.
    private val mirror = Mutex()
    private var hydrated = false
    private var generation = 0
    private val operations = Mutex()

    fun observe(): Flow<CommutePlan> =
        flow {
            hydrate()
            emitAll(state)
        }

    suspend fun refresh(): Outcome<Unit, TransitError> {
        val began = hydrate()
        return operations.withLock {
            if (signedOut(began, "refresh")) return@withLock Outcome.Err(TransitError.SignedOut)
            when (val outcome = service.plan()) {
                is Outcome.Ok -> {
                    if (apply(outcome.value, began)) log.d { "refresh ok directions=${outcome.value.directions.size}" }
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
        val began = hydrate()
        return operations.withLock {
            if (signedOut(began, "save")) return@withLock Outcome.Err(TransitError.SignedOut)
            when (val outcome = service.savePlan(plan)) {
                is Outcome.Ok -> {
                    if (apply(outcome.value, began)) log.i { "save ok directions=${outcome.value.directions.size}" }
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
        val began = hydrate()
        return operations.withLock {
            if (signedOut(began, "remove")) return@withLock Outcome.Err(TransitError.SignedOut)
            when (val outcome = service.removePlan()) {
                is Outcome.Ok -> {
                    if (apply(outcome.value, began)) log.i { "remove ok" }
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
    // the device must not inherit it, not even from a request still in flight
    // or waiting its turn.
    suspend fun clear() {
        mirror.withLock {
            hydrated = true
            generation++
            state.value = CommutePlan()
            storage.remove(STORAGE_KEY)
        }
    }

    // Loads the stored snapshot once and answers the generation a request
    // begins under.
    private suspend fun hydrate(): Int =
        mirror.withLock {
            if (!hydrated) {
                hydrated = true
                storage.get(STORAGE_KEY)
                    ?.let { cached -> runCatching { json.decodeFromString<TransitCommutePlanDTO>(cached) }.getOrNull() }
                    ?.let { state.value = it.toDomain() }
            }
            generation
        }

    // Whether a logout came between the call and its turn.
    private suspend fun signedOut(
        began: Int,
        operation: String,
    ): Boolean =
        mirror.withLock {
            val stale = began != generation
            if (stale) log.i { "$operation skipped: logged out meanwhile" }
            stale
        }

    // False when a logout came first: the answer belongs to the account that asked.
    private suspend fun apply(
        plan: CommutePlan,
        began: Int,
    ): Boolean =
        mirror.withLock {
            if (began != generation) {
                log.i { "answer dropped: logged out meanwhile" }
                return@withLock false
            }
            state.value = plan
            if (plan.isEmpty) {
                storage.remove(STORAGE_KEY)
            } else {
                storage.put(STORAGE_KEY, json.encodeToString(plan.toDto()))
            }
            true
        }

    private companion object {
        const val STORAGE_KEY = "transit.commute_plan"
    }
}
