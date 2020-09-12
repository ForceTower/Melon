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

import android.util.Base64
import com.forcetower.sagres.utils.WordUtils

fun String.makeSemester(): String {
    return if (this.length > 4) {
        if (this[4] == '.') this
        else this.substring(0, 4) + "." + this.substring(4)
    } else {
        this
    }
}

fun String?.toTitleCase(): String? = WordUtils.toTitleCase(this)

fun String.toBase64(): String {
    return Base64.encodeToString(this.toByteArray(), Base64.DEFAULT)
}

fun String?.toBooleanOrNull(): Boolean? {
    if (this == null) return null
    return try {
        java.lang.Boolean.parseBoolean(this)
    } catch (t: Throwable) {
        null
    }
}
