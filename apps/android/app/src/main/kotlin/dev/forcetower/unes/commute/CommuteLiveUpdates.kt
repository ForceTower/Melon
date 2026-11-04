package dev.forcetower.unes.commute

import dev.forcetower.melon.core.common.AppClock
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitLiveUpdateRegistration
import dev.forcetower.melon.feature.transit.domain.model.TransitRoute
import dev.forcetower.melon.feature.transit.domain.usecase.DecodeTransitLiveUpdateUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.RegisterTransitLiveUpdateUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.UnregisterTransitLiveUpdateUseCase
import dev.forcetower.unes.firebase.PushIdentifierStore
import dev.forcetower.unes.ui.feature.commute.CommuteFollowSeed
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

// "Seguindo o 087" outside the app, the Android counterpart of the iOS Live
// Activity. The app draws the ongoing notification from what the follow
// screen showed, registers it with the backend, and from then on redraws it
// from the FCM data messages the backend sends until the bus passes the stop.
@Singleton
internal class CommuteLiveUpdates @Inject constructor(
    private val store: CommuteLiveUpdateStore,
    private val notifier: CommuteLiveUpdateNotifier,
    private val pushIdentifiers: PushIdentifierStore,
    private val registerLiveUpdate: RegisterTransitLiveUpdateUseCase,
    private val unregisterLiveUpdate: UnregisterTransitLiveUpdateUseCase,
    private val decodeLiveUpdate: DecodeTransitLiveUpdateUseCase,
    private val clock: AppClock,
) {
    private val mutex = Mutex()

    // The departure on the notification, if any.
    val followedArrivalId: Flow<String?> = store.record.map { it?.arrivalId }.distinctUntilChanged()

    fun canPost(): Boolean = notifier.canPost()

    suspend fun followed(): CommuteFollowSeed? = store.current()?.followSeed()

    // Replaces whichever bus was followed before. False when there is nothing
    // to show or notifications are off.
    suspend fun start(
        arrival: TransitArrival,
        route: TransitRoute,
        lineName: String,
        stopId: String,
        progressMeters: Double?,
    ): Boolean {
        if (!notifier.canPost()) return false
        val now = clock.now()
        val record = CommuteLiveRecord.of(
            activityId = UUID.randomUUID().toString(),
            arrival = arrival,
            route = route,
            lineName = lineName,
            stopId = stopId,
            progressMeters = progressMeters,
            now = now,
        ) ?: return false
        val previous = mutex.withLock {
            val previous = store.current()
            store.save(record)
            notifier.show(record, now, stale = false)
            notifier.scheduleStale(record.activityId, now + STALE_AFTER)
            previous
        }
        Timber.tag(TAG).i("started line=%s direction=%s", record.lineNumber, record.direction)
        previous?.takeIf { it.registered }?.let { unregister(it.activityId) }
        register(record)
        return true
    }

    suspend fun stop(activityId: String? = null) {
        val record = mutex.withLock {
            val record = store.current() ?: return
            if (activityId != null && record.activityId != activityId) return
            store.clear()
            notifier.cancel()
            record
        }
        Timber.tag(TAG).i("stopped line=%s", record.lineNumber)
        if (record.registered) unregister(record.activityId)
    }

    // An FCM data message for the followed departure. One for another
    // activity (a stop whose unregister never landed) is ignored, and so is
    // one older than what the notification already shows.
    suspend fun onPush(data: Map<String, String>) {
        val activityId = data[KEY_ACTIVITY_ID] ?: return
        val timestamp = data[KEY_TIMESTAMP]?.toLongOrNull() ?: return
        val update = data[KEY_CONTENT]?.let { decodeLiveUpdate(it) } ?: return
        val now = clock.now()
        mutex.withLock {
            val record = store.current()?.takeIf { it.activityId == activityId && timestamp >= it.updatedAt }
                ?: return
            val next = record.copy(update = CommuteLiveRecord.Update.of(update), updatedAt = timestamp)
            if (data[KEY_EVENT] == EVENT_END) {
                end(next, data[KEY_DISMISSAL_DATE]?.toLongOrNull()?.let { Instant.fromEpochSeconds(it) }, now)
                return
            }
            store.save(next)
            notifier.show(next, now, stale = false)
            val staleAt =
                data[KEY_STALE_DATE]?.toLongOrNull()?.let { Instant.fromEpochSeconds(it) } ?: (now + STALE_AFTER)
            notifier.scheduleStale(activityId, staleAt)
        }
    }

    // The stale alarm: no push came in time, so the band stops counting.
    suspend fun onStale(activityId: String) {
        mutex.withLock {
            val record = store.current()?.takeIf { it.activityId == activityId } ?: return
            notifier.show(record, clock.now(), stale = true)
        }
    }

    // Every app open: a registration that never went through (offline, no
    // FCM token yet) is tried again.
    suspend fun resume() {
        val record = store.current() ?: return
        if (!record.registered) register(record)
    }

    // The backend deleted it on its side when it sent the end.
    private suspend fun end(
        record: CommuteLiveRecord,
        dismissAt: Instant?,
        now: Instant,
    ) {
        store.clear()
        notifier.cancelStale()
        if (dismissAt != null && dismissAt <= now) {
            notifier.cancel()
        } else {
            notifier.show(record, now, stale = false, dismissAt = dismissAt)
        }
        Timber.tag(TAG).i("ended by server line=%s status=%s", record.lineNumber, record.update.status)
    }

    private suspend fun register(record: CommuteLiveRecord) {
        val deviceToken = pushIdentifiers.token()
        if (deviceToken == null) {
            // Without it FCM can't reach the device; the notification keeps
            // its first estimate and goes stale on schedule.
            Timber.tag(TAG).w("register skipped: no fcm token yet")
            return
        }
        val outcome = registerLiveUpdate(
            TransitLiveUpdateRegistration(
                activityId = record.activityId,
                deviceToken = deviceToken,
                lineNumber = record.lineNumber,
                direction = TransitDirection.fromWire(record.direction) ?: TransitDirection.ToCampus,
                stopId = record.stopId,
                arrivalId = record.arrivalId,
                vehicleId = record.vehicleId,
            ),
        )
        if (outcome !is Outcome.Ok) return
        mutex.withLock {
            store.current()?.takeIf {
                it.activityId == record.activityId
            }?.let { store.save(it.copy(registered = true)) }
        }
    }

    // Best-effort: the backend ends it anyway when the bus passes or it expires,
    // and pushes for an activity the device no longer follows are ignored.
    private suspend fun unregister(activityId: String) {
        if (unregisterLiveUpdate(activityId) !is Outcome.Ok) {
            Timber.tag(TAG).i("unregister failed; the backend ends it on its own")
        }
    }

    companion object {
        // The `kind` of the backend's data messages for this notification.
        const val PUSH_KIND = "transit_live_activity"

        private const val TAG = "CommuteLiveUpdates"
        private const val KEY_ACTIVITY_ID = "activityId"
        private const val KEY_EVENT = "event"
        private const val KEY_CONTENT = "content"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_STALE_DATE = "staleDate"
        private const val KEY_DISMISSAL_DATE = "dismissalDate"
        private const val EVENT_END = "end"

        // Matches the backend's stale date for the first estimate, before any push.
        private val STALE_AFTER = 3.minutes
    }
}
