package dev.forcetower.unes.firebase

import android.os.Build
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.notifications.domain.usecase.RegisterNotificationTokenUseCase
import dev.forcetower.unes.BuildConfig
import dev.forcetower.unes.di.ApplicationScope
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

// Mirrors the iOS `MessagingDelegate` half of `AppDelegate.swift`: when FCM
// hands us a fresh registration token, push it to the backend if we already
// have a session. If the user isn't logged in yet, do nothing — `SyncViewModel`
// re-fetches the cached token via `FirebaseMessaging.getInstance().token` on
// the auth step right after login and registers it from there.
@AndroidEntryPoint
class MelonFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var registerNotificationToken: RegisterNotificationTokenUseCase
    @Inject lateinit var sessionStore: SessionStore
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onNewToken(token: String) {
        Timber.tag(TAG).i("FCM token received length=%d", token.length)
        applicationScope.launch {
            if (sessionStore.getAccessToken() == null) {
                Timber.tag(TAG).i("FCM token skip register — no session yet")
                return@launch
            }
            val outcome = runCatching {
                registerNotificationToken(
                    token = token,
                    platform = "android",
                    deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                    appVersion = BuildConfig.VERSION_NAME,
                    locale = Locale.getDefault().toLanguageTag(),
                )
            }.getOrElse {
                Timber.tag(TAG).w(it, "FCM token backend register threw")
                return@launch
            }
            when (outcome) {
                is Outcome.Ok -> Timber.tag(TAG).i("FCM token registered with backend")
                is Outcome.Err -> Timber.tag(TAG).w("FCM token register failed err=%s", outcome.error)
            }
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
