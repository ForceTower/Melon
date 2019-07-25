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

import com.forcetower.sagres.database.model.SDisciplineMissedClass
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber

object SagresMissedClassesParser {

    @JvmStatic
    fun extractMissedClasses(document: Document, semesterId: Long): Pair<Boolean, List<SDisciplineMissedClass>> {
        var error = false
        val values: MutableList<SDisciplineMissedClass> = ArrayList()

        try {
            val div = document.selectFirst("div[id=\"divBoletins\"]")
            val classes = div.select("div[class=\"boletim-container\"]")

            for (clazz in classes) {
                val info = clazz.selectFirst("div[class=\"boletim-item-info\"]")
                val name = info.selectFirst("span[class=\"boletim-item-titulo cor-destaque\"]")

                val text = name.text()
                val code = text.substring(0, text.indexOf("-") - 1).trim()

                val frequency = clazz.selectFirst("div[class=\"boletim-frequencia\"]")
                val spectrum = frequency.selectFirst("table")
                if (spectrum == null) Timber.d("<table_not_found> :: There's no missed classes for $code")
                else {
                    val body = spectrum.selectFirst("tbody")
                    if (body == null) Timber.d("<body_not_found> :: There's no missed classes for $code")
                    else values.addAll(fourier(body, code, semesterId))
                }
            }
        } catch (t: Throwable) {
            Timber.d("Throwable T: ${t.message}")
            error = true
        }

        return Pair(error, values)
    }

    @JvmStatic
    private fun fourier(element: Element, code: String, semesterId: Long): List<SDisciplineMissedClass> {
        val values: MutableList<SDisciplineMissedClass> = ArrayList()
        val indexes = element.select("tr")

        for (index in indexes) {
            val information = index.child(0).child(0).children()
            val date = information[0].text().trim()
            val desc = information[1].text().trim()
            values.add(SDisciplineMissedClass(date, desc, code, semesterId))
        }
        return values
    }
}