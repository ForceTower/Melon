package dev.forcetower.unes.ui.feature.commute

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

// The while-in-use prompt, raised only from a button the student taps. An
// already granted permission skips the dialog; a denial lands on `onDenied`
// so the screen can say location is optional and point at Settings.
@Composable
internal fun rememberLocationRequest(
    permissions: Array<String>,
    isGranted: () -> Boolean,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): () -> Unit {
    val granted = rememberUpdatedState(onGranted)
    val denied = rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) granted.value() else denied.value()
    }
    return remember(launcher, permissions) {
        {
            if (isGranted()) granted.value() else launcher.launch(permissions)
        }
    }
}

// "Permitir em Configurações" — the app's own page in the system settings.
internal fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
