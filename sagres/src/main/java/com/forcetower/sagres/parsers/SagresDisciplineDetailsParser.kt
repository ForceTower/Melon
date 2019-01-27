/*
 * Copyright (c) 2019.
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

import com.forcetower.sagres.database.model.SDisciplineClassItem
import com.forcetower.sagres.database.model.SDisciplineClassLocation
import com.forcetower.sagres.database.model.SDisciplineGroup
import com.forcetower.sagres.utils.ValueUtils.toInteger
import com.forcetower.sagres.utils.WordUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import timber.log.Timber
import java.util.ArrayList

object SagresDisciplineDetailsParser {

    @JvmStatic
    fun extractDisciplineGroup(document: Document): SDisciplineGroup? {
        val elementName = document.selectFirst("h2[class=\"cabecalho-titulo\"]")
        if (elementName == null) {
            Timber.d("Element name is null")
            Timber.d(document.text())
            Timber.d("Element name is null")
            return null
        }

        val classNameFull = elementName.text()

        val codePos = classNameFull.indexOf("-")
        val code = classNameFull.substring(0, codePos).trim { it <= ' ' }
        val groupPos = classNameFull.lastIndexOf("(")
        val group = classNameFull.substring(groupPos)
        val refGroupPos = group.lastIndexOf("-")
        val refGroup = group.substring(refGroupPos + 1, group.length - 1).trim { it <= ' ' }
        val name = classNameFull.substring(codePos + 1, groupPos).trim { it <= ' ' }
        var teacher = ""
        var elementTeacher: Element? = document.selectFirst("div[class=\"cabecalho-dado nome-capitalizars\"]")
        if (elementTeacher != null) {
            elementTeacher = elementTeacher.selectFirst("span")
            if (elementTeacher != null) teacher = WordUtils.toTitleCase(elementTeacher.text())
        }

        var semesterByName: String? = null
        var classCredits = ""
        var missLimits = ""
        var classPeriod: String? = null
        var department: String? = null
        val locations = mutableListOf<SDisciplineClassLocation>()

        for (element in document.select("div[class=\"cabecalho-dado\"]")) {
            val b = element.child(0)
            val bText = b.text()
            if (bText.equals("Período:", ignoreCase = true)) {
                semesterByName = element.child(1).text()
            } else if (bText.equals("Carga horária:", ignoreCase = true) && classCredits.isEmpty()) {
                classCredits = element.child(1).text()
                classCredits = classCredits.replace("[^\\d]".toRegex(), "").trim()
            } else if (bText.equals("Limite de Faltas:", ignoreCase = true)) {
                missLimits = element.child(1).text()
                missLimits = missLimits.replace("[^\\d]".toRegex(), "").trim()
            } else if (bText.equals("Período de aulas:", ignoreCase = true)) {
                classPeriod = element.selectFirst("span").text()
            } else if (bText.equals("Departamento:", ignoreCase = true)) {
                department = WordUtils.toTitleCase(element.child(1).text())
            } else if (bText.equals("Horário:", ignoreCase = true)) {
                for (classTime in element.select("div[class=\"cabecalho-horario\"]")) {
                    val day = classTime.child(0).text()
                    val start = classTime.child(1).text()
                    val end = classTime.child(3).text()
                    locations.add(SDisciplineClassLocation(start, end, day, null, null, null, name, code, group, true))
                }
            }
        }

        if (classCredits.isEmpty()) classCredits = "0"
        if (missLimits.isEmpty()) missLimits = "0"
        var credits = 0
        var maxMiss = 0
        try {
            credits = Integer.parseInt(classCredits)
            maxMiss = Integer.parseInt(missLimits)
        } catch (e: Exception) {
            Timber.d("Exception in parse int for numbers")
        }

        val created = SDisciplineGroup(teacher, refGroup, credits, maxMiss, classPeriod, department, locations)
        created.setDisciplineCodeAndSemester(code, semesterByName)
        created.classItems = extractClassItems(document)
        created.isDraft = false
        return created
    }

    @JvmStatic
    fun extractClassItems(document: Document): List<SDisciplineClassItem> {
        val items = ArrayList<SDisciplineClassItem>()

        val trs = document.select("tr[class]")
        for (tr in trs) {
            if (tr.attr("id").contains("header")) continue

            val tds = tr.children()
            if (!tds.isEmpty()) {
                val classItem = getFromTDs(tds)
                if (classItem != null) items.add(classItem)
            }
        }

        return items
    }

    @JvmStatic
    private fun getFromTDs(tds: Elements): SDisciplineClassItem? {
        try {
            val strNumber = tds[0].text().trim()
            val situation = tds[1].text()
            val date = tds[2].text()
            val description = tds[3].text()
            val strMaterials = tds[5].text().trim()
            val number = toInteger(strNumber, -1)
            val materials = toInteger(strMaterials, -1)

            if (materials > 0) {
                Timber.d("Has $materials materials at $description")
            }

            // Download Material section
            var element = tds[5]
            element = element.selectFirst("a")
            var href = element.attr("HREF")
            if (href.isEmpty()) href = element.attr("href")
            val link = if (href.startsWith("link?")) href.substring(5) else href
            return SDisciplineClassItem(number, situation, description, date, materials, link)
        } catch (ignored: Exception) { ignored.printStackTrace() }
        return null
    }
}