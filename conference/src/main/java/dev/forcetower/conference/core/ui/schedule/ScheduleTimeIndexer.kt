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

package dev.forcetower.conference.core.ui.schedule

import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import dev.forcetower.conference.core.model.persistence.Session
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Find the first session at each start time (rounded down to nearest minute) and return pairs of
 * index to start time. Assumes that [sessions] are sorted by ascending start time.
 */

fun indexSessionHeaders(sessions: List<Session>): List<Pair<Int, ZonedDateTime>> {
    return sessions
        .mapIndexed { index, session ->
            index to session.startTime
        }
        .distinctBy { it.second.truncatedTo(ChronoUnit.MINUTES) }
}

fun newStaticLayout(
    source: CharSequence,
    paint: TextPaint,
    width: Int,
    alignment: Layout.Alignment,
    spacingmult: Float,
    spacingadd: Float,
    includepad: Boolean
): StaticLayout {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        StaticLayout.Builder.obtain(source, 0, source.length, paint, width).apply {
            setAlignment(alignment)
            setLineSpacing(spacingadd, spacingmult)
            setIncludePad(includepad)
        }.build()
    } else {
        @Suppress("DEPRECATION")
        (StaticLayout(source, paint, width, alignment, spacingmult, spacingadd, includepad))
    }
}
