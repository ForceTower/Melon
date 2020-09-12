/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.feature.shared.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

fun Context.isNavBarOnBottom(): Boolean {
    val config = resources.configuration
    val dm = resources.displayMetrics
    val canMove = dm.widthPixels != dm.heightPixels && config.smallestScreenWidthDp < 600
    return !canMove || dm.widthPixels < dm.heightPixels
}

fun Context.openURL(url: String) {
    var fixed = url
    if (!url.startsWith("http://") &&
        !url.startsWith("HTTP://") &&
        !url.startsWith("HTTPS://") &&
        !url.startsWith("https://") &&
        !url.contains("//")
    ) {
        fixed = "http://$url"
    }

    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(fixed)
    this.startActivity(intent)
}

fun isNougatMR1(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
}
