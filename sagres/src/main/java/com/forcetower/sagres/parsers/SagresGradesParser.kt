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

import com.forcetower.sagres.database.model.SCourseVariant
import com.forcetower.sagres.database.model.SGrade
import com.forcetower.sagres.database.model.SGradeInfo
import org.jsoup.nodes.Document
import timber.log.Timber

object SagresGradesParser {
    @JvmStatic
    fun extractSemesterCodes(document: Document): List<Pair<Long, String>> {
        val list: MutableList<Pair<Long, String>> = ArrayList()
        val semestersValues = document.selectFirst("select[id=\"ctl00_MasterPlaceHolder_ddPeriodosLetivos_ddPeriodosLetivos\"]")
        if (semestersValues != null) {
            val options = semestersValues.select("option")
            for (option in options) {
                val value = option.attr("value").trim()
                val semester = option.text().trim()
                try {
                    val semesterId = value.toLong()
                    val pair = Pair(semesterId, semester)
                    list.add(pair)
                } catch (e: Exception) {
                    Timber.d("Can't parse long: $value")
                }
            }
            return list
        } else {
            //TODO place crashlytics here
            return list
        }
    }

    @JvmStatic
    fun getSelectedSemester(document: Document): Pair<Boolean, Long>? {
        val values = document.select("option[selected=\"selected\"]")
        return if (values.size == 1) {
            val value = values[0].attr("value").trim()
            try {
                val id = value.toLong()
                Pair(true, id)
            } catch (e: Exception) {
                Timber.d("Can't parse long: $value")
                null
            }
        } else {
            val defValue = document.selectFirst("select[id=\"ctl00_MasterPlaceHolder_ddPeriodosLetivos_ddPeriodosLetivos\"]")
            if (defValue != null) {
                val selected = defValue.selectFirst("option[selected=\"selected\"]")
                if (selected != null) {
                    val value = selected.attr("value").trim()
                    try {
                        val id = value.toLong()
                        Timber.d("Successfully found current semester using the alternate way")
                        Pair(false, id)
                    } catch (e: Exception) {
                        Timber.d("Can't parse long: $value")
                    }
                }
            }
            null
        }
    }

    @JvmStatic
    fun extractCourseVariants(document: Document): List<SCourseVariant> {
        val courses: MutableList<SCourseVariant> = ArrayList()
        val variants = document.selectFirst("select[id=\"ctl00_MasterPlaceHolder_ddRegistroCurso\"]")
        if (variants != null) {
            val elements = variants.children()
            for (element in elements) {
                try {
                    val uefsId = element.attr("value").toLong()
                    val name   = element.text().trim()
                    courses.add(SCourseVariant(uefsId, name))
                    Timber.d("Added variant id: $uefsId with name: $name")
                } catch (e: Exception) {}
            }
        }

        return courses
    }

    @JvmStatic
    fun canExtractGrades(document: Document): Boolean {
        val bulletin = document.selectFirst("div[id=\"divBoletins\"]")
        return if (bulletin != null) {
            bulletin.selectFirst("div[class=\"boletim-container\"]") != null
        } else {
            false
        }
    }

    @JvmStatic
    fun extractGrades(document: Document, semesterId: Long): List<SGrade> {
        val grades: MutableList<SGrade> = ArrayList()
        val bulletin = document.selectFirst("div[id=\"divBoletins\"]")
        val classes  = bulletin.select("div[class=\"boletim-container\"]")

        for (clazz in classes) {
            try {
                val info = clazz.selectFirst("div[class=\"boletim-item-info\"]")
                val name = info.selectFirst("span[class=\"boletim-item-titulo cor-destaque\"]")

                val discipline = name.text().trim()
                val grade = SGrade(semesterId, discipline)

                val gradeInfo = clazz.selectFirst("div[class=\"boletim-notas\"]")
                val table = gradeInfo.selectFirst("table")
                val body  = table.selectFirst("tbody")

                if (body != null) {
                    val trs = body.select("tr")
                    for (tr in trs) {
                        val children = tr.children()
                        if (children.size == 4) {
                            val td = children[0]
                            if (td.children().size == 0) {
                                val mean = children[2]
                                grade.partialMean = mean.text().trim()
                            } else {
                                val date = children[0].text().trim()
                                val evaluation = children[1].text().trim()
                                val score = children[2].text().trim()
                                var weight = children[3].text().trim().toDoubleOrNull()
                                if (weight == null) weight = 1.0
                                grade.addInfo(SGradeInfo(evaluation, score, date, weight))
                            }
                        }
                    }
                    grades.add(grade)
                } else {
                    Timber.d("<body_is_null> :: Can't parse grades")
                }

                val foot = table.selectFirst("tfoot")
                if (foot != null) {
                    val tr = foot.selectFirst("tr")
                    if (tr != null && tr.children().size == 4)
                        grade.finalScore = tr.children()[2].text().trim()
                }
            } catch (t: Throwable) {
                Timber.d("Exception happened")
                t.printStackTrace()
            }
        }

        return grades
    }
}