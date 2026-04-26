package dev.forcetower.melon.core.session.data

import dev.forcetower.melon.core.common.ApplicationContext
import platform.UIKit.UIDevice

internal actual fun platformDeviceId(appContext: ApplicationContext): String? =
    UIDevice.currentDevice.identifierForVendor?.UUIDString?.lowercase()?.replace("-", "")
