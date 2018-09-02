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