package dev.forcetower.unes.ui.feature.onboarding.intro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Returns a callback that requests POST_NOTIFICATIONS on Android 13+ and is a
 * no-op on earlier API levels (notification permission is implicit there).
 *
 * Mirrors iOS `IntroCarouselView.requestNotificationsAuthorizationIfNeeded()`:
 * fire-and-forget — the carousel immediately advances, regardless of whether
 * the user grants or denies. The system dialog is throttled by the OS so calling
 * this from a no-op state (already-granted or previously-denied) is safe.
 */
@Composable
fun rememberRequestNotificationPermission(): () -> Unit {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return remember { {} }
    }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* fire-and-forget */ },
    )
    return remember(launcher) {
        {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
