/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.sagres.parsers

import com.forcetower.sagres.database.model.SCalendar
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.ArrayList

/**
 * Created by João Paulo on 06/03/2018.
 */

object SagresCalendarParser {

    @JvmStatic
    fun getCalendar(document: Document): List<SCalendar>? {
        val element = document.selectFirst("div[class=\"webpart-calendario\"]")
        if (element == null) {
            Timber.d("Calendar not found")
            return null
        }

        if (element.childNodeSize() < 2) {
            Timber.d("Calendar found, but not able to parse")
            return null
        }

        val items = ArrayList<SCalendar>()
        val events = element.child(1)
        val ul = events.selectFirst("ul")

        for (li in ul.select("li")) {
            val text = li.text()
            val index = text.indexOf("-")
            val days = text.substring(0, index)
            val event = text.substring(index + 1)
            items.add(SCalendar(days, event.trim { it <= ' ' }))
        }

        return items
    }
}
