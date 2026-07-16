package dev.forcetower.unes.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import dev.forcetower.unes.di.ApplicationScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

// Mirrors the iOS `MessagingDelegate` half of `AppDelegate.swift`: FCM hands
// us the Firebase Installation ID via onRegistered (enabled by the manifest
// meta-data; FCM retired registration-token targeting). The FID is cached,
// then the registrar pushes it to the backend. Without a session it skips —
// `SyncViewModel` reconciles right after login.
@AndroidEntryPoint
class MelonFirebaseMessagingService : FirebaseMessagingService() {

    @Inject internal lateinit var identifierStore: PushIdentifierStore
    @Inject internal lateinit var pushRegistrar: PushRegistrar
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onRegistered(installationId: String) {
        Timber.tag(TAG).i("FID received length=%d", installationId.length)
        applicationScope.launch {
            runCatching { identifierStore.setFid(installationId) }
                .onFailure { Timber.tag(TAG).w(it, "fid persist failed") }
            pushRegistrar.reconcile()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Background pushes with a `notification` payload are auto-displayed
        // by the system before this callback fires. Only data-only messages
        // land here in the background; any payload lands here in foreground.
        Timber.tag(TAG).i("remote message from=%s data=%s", message.from, message.data.keys)
    }

    private companion object {
        const val TAG = "MelonFcmService"
    }
}
