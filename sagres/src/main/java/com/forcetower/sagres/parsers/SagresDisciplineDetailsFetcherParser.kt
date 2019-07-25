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

import okhttp3.FormBody
import okhttp3.RequestBody
import org.jsoup.nodes.Document
import java.util.ArrayList
import timber.log.Timber

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

                    val fixedSemester = classSemester?.replace(".", "")
                    val fixedPeriod = period.replace(".", "")

                    if (fixedSemester == null || fixedPeriod.equals(fixedSemester, ignoreCase = true)) {
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

                val fixedSemester = classSemester?.replace(".", "")
                val fixedPeriod = period.replace(".", "")

                if (fixedSemester == null || fixedPeriod.equals(fixedSemester, ignoreCase = true)) {
                    if (classCode == null || code.equals(classCode, ignoreCase = true)) {
                        builders.add(builder to period)
                    }
                }
            }
        }
        return builders
    }

    @JvmStatic
    fun extractParamsForDiscipline(document: Document, xpecial: Boolean = false): FormBody.Builder {
        val builder = FormBody.Builder()
        val elements = document.select("input[value][type=\"hidden\"]")
        builder.add("ctl00\$MasterPlaceHolder\$RowsPerPage1\$ddMostrar", "0")
        for (element in elements) {
            val id = element.attr("id")
            val value = element.attr("value")
            if (!xpecial || id != "ctl00\$MasterPlaceHolder\$RowsPerPage1\$ddMostrar") {
                builder.add(id, value)
            }
        }
        return builder
    }

    @JvmStatic
    fun extractFastForms(
        document: Document,
        classSemester: String?,
        classCode: String?,
        classGroup: String?
    ): List<RequestBody?>? {
        try {
            val elements = document.select("tr").filter { it.attr("id").startsWith("objdwForm_detail_") }
            return elements.map { row ->
                val index = (row.attr("id").split("_")[2].toIntOrNull() ?: 0) + 1

                try {
                    val semester = row.child(1).text().trim()
                    if (classSemester == null || semester.replace(".", "").equals(classSemester.replace(".", ""), ignoreCase = true)) {
                        val fullName = row.child(2).text().trim()
                        val code = fullName.substring(0, fullName.indexOf("-")).trim()
                        if (classCode == null || code.equals(classCode, ignoreCase = true)) {
                            val groups = fullName.substring(fullName.lastIndexOf("(") + 1, fullName.length - 1).trim()
                            val group = groups.substring(groups.lastIndexOf("-") + 1).trim()
                            if (classGroup == null || group.equals(classGroup, ignoreCase = true)) {
                                extractFastFormBodies(index.toString(), document)
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } catch (t: Throwable) {
                    Timber.e(t)
                    null
                }
            }.filter { it != null }
        } catch (t: Throwable) {
            Timber.e(t)
        }

        return null
    }

    @JvmStatic
    private fun extractFastFormBodies(pos: String, document: Document): RequestBody {
        val formBody = FormBody.Builder()
        extractHiddenFields(document, formBody, listOf(
                "__EVENTTARGET",
                "__EVENTARGUMENT",
                "ctl00_MasterPlaceHolder_FiltroClasses_txbFiltroNome",
                "ctl00_MasterPlaceHolder_ctl00_ddMostrar"
        ))
        formBody.add("__EVENTTARGET", "Selecionar")
        formBody.add("__EVENTARGUMENT", pos)
        formBody.add("ctl00_MasterPlaceHolder_FiltroClasses_txbFiltroNome", "")
        formBody.add("ctl00_MasterPlaceHolder_ctl00_ddMostrar", "0")
        return formBody.build()
    }

    @JvmStatic
    private fun extractHiddenFields(document: Document, formBody: FormBody.Builder, ignored: List<String>) {
        val elements = document.select("input[value][type=\"hidden\"]")
        for (element in elements) {
            val key = element.attr("id")
            val value = element.attr("value")
            if (!ignored.contains(key)) formBody.add(key, value)
        }
    }

    @JvmStatic
    fun extractSemesters(document: Document): List<Pair<Long, String>> {
        val list: MutableList<Pair<Long, String>> = ArrayList()
        val semestersValues = document.selectFirst("select[id=\"ctl00_MasterPlaceHolder_FiltroClasses_ddPeriodosLetivos\"]")
        semestersValues ?: return list
        val options = semestersValues.select("option[group]")
        for (option in options) {
            val value = option.attr("value").trim()
            val semester = option.text().trim()
            if (semester.isNotEmpty()) {
                try {
                    val semesterId = value.toLong()
                    val pair = Pair(semesterId, semester)
                    list.add(pair)
                } catch (e: Exception) {
                    Timber.d("Can't parse long: $value")
                }
            }
        }
        return list
    }
}