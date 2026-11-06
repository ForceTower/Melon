package dev.forcetower.unes.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import dev.forcetower.unes.commute.CommuteLiveUpdates
import dev.forcetower.unes.di.ApplicationScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

// Mirrors the iOS `MessagingDelegate` half of `AppDelegate.swift`: FCM hands
// us the registration token via onNewToken; the registrar caches it and
// pushes it to the backend. Without a session it skips — `SyncViewModel`
// reconciles right after login.
@AndroidEntryPoint
internal class MelonFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushRegistrar: PushRegistrar
    @Inject lateinit var pushSyncCoordinator: PushSyncCoordinator
    @Inject lateinit var commuteLiveUpdates: CommuteLiveUpdates
    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    // Deprecated in favor of FID targeting, but FID-targeted sends stopped
    // reaching some devices — the clients are fully back on registration
    // tokens until FID push matures.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        Timber.tag(TAG).i("FCM token received length=%d", token.length)
        applicationScope.launch {
            pushRegistrar.tokenReceived(token)
            commuteLiveUpdates.resume()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Background pushes with a `notification` payload are auto-displayed
        // by the system before this callback fires. Only data-only messages
        // land here in the background; any payload lands here in foreground.
        Timber.tag(TAG).i("remote message from=%s data=%s", message.from, message.data.keys)
        // The followed bus's ongoing notification redraws itself; nothing in
        // the mirror changed, so it must not trigger a sync.
        if (message.data["kind"] == CommuteLiveUpdates.PUSH_KIND) {
            applicationScope.launch { commuteLiveUpdates.onPush(message.data) }
            return
        }
        // Foreground receipt of a mirror-relevant push (spec 0008): `kind`
        // certifies upstream data changed, so pull it now — no ON_START will
        // fire while the app stays open. Same guard as iOS `PushClient`:
        // no `kind`, no refresh.
        if (message.data["kind"] != null) {
            pushSyncCoordinator.request()
        }
    }

    private companion object {
        const val TAG = "MelonFcmService"
    }
}
