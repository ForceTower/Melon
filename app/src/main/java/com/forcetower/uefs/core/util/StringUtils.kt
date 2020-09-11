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

package com.forcetower.uefs.core.util

import android.util.Patterns
import java.text.Normalizer
import java.util.ArrayList

fun String.getLinks(): List<String> {
    val matcher = Patterns.WEB_URL.matcher(this)
    val links = ArrayList<String>()

    if (this.isBlank()) return links

    while (matcher.find()) {
        val matchStart = matcher.start(1)
        val matchEnd = matcher.end()
        links.add(this.substring(matchStart, matchEnd))
    }
    return links
}

fun String.unaccent(): String {
    return removeAccents(this)
}

fun removeAccents(src: String): String {
    return Normalizer
        .normalize(src, Normalizer.Form.NFD)
        .replace(Regex("[^\\p{ASCII}]"), "")
}
