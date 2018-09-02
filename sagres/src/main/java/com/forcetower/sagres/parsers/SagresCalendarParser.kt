/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
