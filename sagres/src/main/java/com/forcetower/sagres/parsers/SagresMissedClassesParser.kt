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
                val spectrum  = frequency.selectFirst("table")
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