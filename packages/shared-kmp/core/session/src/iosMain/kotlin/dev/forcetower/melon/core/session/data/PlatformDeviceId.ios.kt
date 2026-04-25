package dev.forcetower.melon.core.session.data

import platform.UIKit.UIDevice

internal actual fun platformDeviceId(): String? =
    UIDevice.currentDevice.identifierForVendor?.UUIDString?.lowercase()?.replace("-", "")
