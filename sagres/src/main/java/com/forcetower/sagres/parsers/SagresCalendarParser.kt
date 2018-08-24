/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.sagres.parsers

import com.forcetower.sagres.database.model.SCalendar

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

import java.util.ArrayList
import timber.log.Timber

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
