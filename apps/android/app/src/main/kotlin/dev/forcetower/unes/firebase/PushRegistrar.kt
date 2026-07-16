package dev.forcetower.unes.firebase

import android.os.Build
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.notifications.domain.usecase.RegisterNotificationTokenUseCase
import dev.forcetower.melon.feature.notifications.domain.usecase.UnregisterNotificationTokenUseCase
import dev.forcetower.unes.BuildConfig
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

// Owns the device's push registration: the Firebase Installation ID is
// registered with the backend, and any row left behind by the retired
// FCM-token path is deleted. The backend dedups rows by identifier value, so
// the token row a previous app version registered stays live next to the FID
// row — and every push would arrive twice — until the delete lands; failed
// deletes are queued and retried on later reconciles.
@Singleton
internal class PushRegistrar @Inject constructor(
    private val identifierStore: PushIdentifierStore,
    private val sessionStore: SessionStore,
    private val registerNotificationToken: RegisterNotificationTokenUseCase,
    private val unregisterNotificationToken: UnregisterNotificationTokenUseCase,
) {
    private val mutex = Mutex()

    suspend fun reconcile() {
        try {
            mutex.withLock { reconcileLocked() }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (ex: Throwable) {
            Timber.tag(TAG).w(ex, "reconcile failed")
        }
    }

    private suspend fun reconcileLocked() {
        if (sessionStore.getAccessToken() == null) {
            Timber.tag(TAG).i("reconcile skipped — no session yet")
            return
        }
        val fid = identifierStore.fid()
        if (fid.isNullOrEmpty()) {
            flushPendingDeletes()
            return
        }
        val outcome = registerNotificationToken(
            token = fid,
            identifierType = "fid",
            platform = "android",
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            appVersion = BuildConfig.VERSION_NAME,
            locale = Locale.getDefault().toLanguageTag(),
        )
        when (outcome) {
            is Outcome.Ok -> {
                Timber.tag(TAG).i("fid registered with backend")
                // Queue the legacy token's delete only after the FID is
                // registered — deleting first would leave a push gap.
                val legacyToken = identifierStore.token()?.takeIf { it != fid }
                if (legacyToken != null) {
                    identifierStore.addPendingDelete(legacyToken)
                    identifierStore.clearToken()
                }
            }
            is Outcome.Err -> {
                Timber.tag(TAG).w("fid register failed err=%s", outcome.error)
                return
            }
        }
        flushPendingDeletes()
    }

    // Logout teardown — must run while the session bearer is still valid.
    // Best-effort: after logout there is nothing to retry with; the backend's
    // invalid-token prune eventually collects what this misses.
    suspend fun unregisterAll() {
        mutex.withLock {
            val identifiers = buildSet {
                identifierStore.fid()?.let(::add)
                identifierStore.token()?.let(::add)
                addAll(identifierStore.pendingDeletes())
            }
            for (identifier in identifiers) {
                val outcome = runCatching { unregisterNotificationToken(identifier) }.getOrNull()
                if (outcome !is Outcome.Ok) {
                    Timber.tag(TAG).w("logout unregister failed for a push identifier")
                }
            }
            identifierStore.clear()
        }
    }

    private suspend fun flushPendingDeletes() {
        for (identifier in identifierStore.pendingDeletes()) {
            when (unregisterNotificationToken(identifier)) {
                is Outcome.Ok -> identifierStore.removePendingDelete(identifier)
                is Outcome.Err -> Timber.tag(TAG).w("stale push identifier delete failed — will retry")
            }
        }
    }

    private companion object {
        const val TAG = "PushRegistrar"
    }
}
