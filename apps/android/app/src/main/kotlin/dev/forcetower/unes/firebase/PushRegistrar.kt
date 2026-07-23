package dev.forcetower.unes.firebase

import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
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
import kotlinx.coroutines.tasks.await
import timber.log.Timber

// Owns the device's push registration: the FCM registration token is
// registered with the backend on every app foreground, and any Firebase
// Installation ID row left behind by the retired FID-targeting builds is
// deleted (FID-targeted sends stopped reaching some devices, so the clients
// are fully back on tokens until FID push matures). The backend dedups rows
// by identifier value, so a stale identifier's row stays live next to the
// token row — and every push would arrive twice — until the delete lands;
// failed deletes are queued and retried on later reconciles.
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

    // FCM rotated the registration token: retire the old row once the new
    // token is registered, never before — deleting first would leave a push
    // gap.
    suspend fun tokenReceived(token: String) {
        mutex.withLock {
            val previous = identifierStore.token()
            if (previous != null && previous != token) {
                identifierStore.addPendingDelete(previous)
            }
            identifierStore.setToken(token)
        }
        reconcile()
    }

    private suspend fun reconcileLocked() {
        if (sessionStore.getAccessToken() == null) {
            Timber.tag(TAG).i("reconcile skipped — no session yet")
            return
        }
        val token = currentToken()
        if (token.isNullOrEmpty()) {
            Timber.tag(TAG).i("no fcm token available yet")
            flushPendingDeletes()
            return
        }
        val outcome = registerNotificationToken(
            token = token,
            identifierType = "fcm_token",
            platform = "android",
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            appVersion = BuildConfig.VERSION_NAME,
            locale = Locale.getDefault().toLanguageTag(),
        )
        when (outcome) {
            is Outcome.Ok -> {
                Timber.tag(TAG).i("fcm token registered with backend")
                // The FID builds queued this token's delete when they took
                // over — it's canonical again, so the queued delete would
                // strand the device.
                identifierStore.removePendingDelete(token)
                // Queue the FID row's delete only after the token is
                // registered — deleting first would leave a push gap.
                val fid = identifierStore.fid()?.takeIf { it != token }
                if (fid != null) {
                    identifierStore.addPendingDelete(fid)
                    identifierStore.clearFid()
                }
            }
            is Outcome.Err -> {
                Timber.tag(TAG).w("fcm token register failed err=%s", outcome.error)
                return
            }
        }
        flushPendingDeletes()
    }

    // Pull-based on purpose: onNewToken only fires on rotation, and installs
    // that went through the FID-only builds had their cached token cleared.
    // getToken is deprecated in favor of FID targeting, but FID targeting is
    // exactly what broke delivery — the token path needs it.
    @Suppress("DEPRECATION")
    private suspend fun currentToken(): String? {
        val fetched = runCatching { FirebaseMessaging.getInstance().token.await() }
            .onFailure { Timber.tag(TAG).w(it, "fcm token fetch failed — falling back to cache") }
            .getOrNull()
        if (fetched.isNullOrEmpty()) return identifierStore.token()
        val cached = identifierStore.token()
        if (cached != null && cached != fetched) {
            identifierStore.addPendingDelete(cached)
        }
        identifierStore.setToken(fetched)
        return fetched
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
