package com.forcetower.uefs.core.util

import android.os.Build
import com.forcetower.uefs.BuildConfig

object VersionUtils {
    fun isOreo() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
}