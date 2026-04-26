package dev.forcetower.melon.core.session.data

import android.provider.Settings
import dev.forcetower.melon.core.common.ApplicationContext

internal actual fun platformDeviceId(appContext: ApplicationContext): String? =
    Settings.Secure.getString(
        appContext.context.contentResolver,
        Settings.Secure.ANDROID_ID,
    )?.takeIf { it.isNotBlank() }
