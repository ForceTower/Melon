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

import okhttp3.FormBody
import org.jsoup.nodes.Document
import java.util.ArrayList

object SagresDisciplineDetailsFetcherParser {

    @JvmStatic
    fun extractFormBodies(
        document: Document,
        classSemester: String?,
        classCode: String?,
        classGroup: String?
    ): List<Pair<FormBody.Builder, String>> {
        val builders: MutableList<Pair<FormBody.Builder, String>> = ArrayList()

        val elements = document.select("input[value][type=\"hidden\"]")
        val classes = document.select("section[class=\"webpart-aluno-item\"]")

        for (clazz in classes) {
            val title = clazz.selectFirst("a[class=\"webpart-aluno-nome cor-destaque\"]").text()
            val period = clazz.selectFirst("span[class=\"webpart-aluno-periodo\"]").text()

            val code = title.substring(0, title.indexOf("-")).trim()
            val ul = clazz.selectFirst("ul")

            if (ul != null) {
                val lis = ul.select("li")
                for (li in lis) {
                    val element = li.selectFirst("a[href]")
                    var values = element.attr("href")
                    val start = values.indexOf("'")
                    values = values.substring(start + 1)
                    val end = values.indexOf("'")

                    values = values.substring(0, end)
                    var type = element.text()
                    val refGroupPos = type.lastIndexOf("(")
                    type = type.substring(0, refGroupPos).trim { it <= ' ' }

                    if (classSemester == null || period.equals(classSemester, ignoreCase = true)) {
                        if (classCode == null || code.equals(classCode, ignoreCase = true)) {
                            if (classGroup == null || classGroup.equals("unique", ignoreCase = true) || type.equals(classGroup, ignoreCase = true)) {
                                val builderIn = FormBody.Builder()
                                for (elementIn in elements) {
                                    val key = elementIn.attr("id")
                                    val value = elementIn.attr("value")
                                    if (!key.equals("__EVENTTARGET", ignoreCase = true))
                                        builderIn.add(key, value)
                                }
                                builderIn.add("__EVENTTARGET", values)
                                builders.add(builderIn to period)
                            }
                        }
                    }
                }

            } else {
                val webPart = clazz.selectFirst("div[class=\"webpart-dropdown webpart-dropdown-up\"]")
                val anchor = webPart.selectFirst("a[href]")
                var values = anchor.attr("href")
                val start = values.indexOf("'")
                values = values.substring(start + 1)
                val end = values.indexOf("'")

                values = values.substring(0, end)

                val builder = FormBody.Builder()
                builder.add("__EVENTTARGET", values)

                for (element in elements) {
                    val key = element.attr("id")
                    val value = element.attr("value")
                    if (!key.equals("__EVENTTARGET", ignoreCase = true))
                        builder.add(key, value)
                }
                if (classSemester == null || period.equals(classSemester, ignoreCase = true)) {
                    if (classCode == null || code.equals(classCode, ignoreCase = true)) {
                        builders.add(builder to period)
                    }
                }
            }
        }
        return builders
    }

    @JvmStatic
    fun extractParamsForDiscipline(document: Document): FormBody.Builder {
        val builder = FormBody.Builder()
        val elements = document.select("input[value][type=\"hidden\"]")
        builder.add("ctl00\$MasterPlaceHolder\$RowsPerPage1\$ddMostrar", "0")
        for (element in elements) {
            val id = element.attr("id")
            val value = element.attr("value")
            builder.add(id, value)
        }
        return builder
    }
}