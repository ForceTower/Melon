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

import com.forcetower.sagres.database.model.SDemandOffer
import org.jsoup.nodes.Document

object SagresDemandParser {
    @JvmStatic
    fun getOffers(document: Document): List<SDemandOffer> {
        val list = mutableListOf<SDemandOffer>()

        //Each element represents a column full of disciplines for the semester
        val elements = document.select("div[class=\"demanda-coluna\"]")
        for (element in elements) {
            val title = element.selectFirst("span").text().trim()

            val disciplines = element.select("div[qtcredito]")
            for (discipline in disciplines) {
                val id = discipline.selectFirst("input[type=\"hidden\"]").attr("name").trim()
                val selected = discipline.selectFirst("input[type=\"hidden\"]").attr("value").trim()
                val name = discipline.attr("nomeatividade").trim()
                val code = discipline.attr("codigoatividade").trim()
                //val activityId = discipline.attr("idatividade").trim()
                val hour = discipline.attr("qthoras").trim()

                var av = false
                var co = false
                var un = false
                var se = true
                var cu = false

                val clazz = discipline.attr("class").trim().split(" ")
                when {
                    clazz.contains("demanda-atividade-disponivel") -> av = true
                    clazz.contains("demanda-atividade-cumprida") -> co = true
                    clazz.contains("demanda-atividade-indisponivel") -> un = true
                    else -> se = false
                }

                val coursing = discipline.selectFirst("div[class=\"  demanda-atividade-cursando\"]")
                if (coursing != null) cu = true

                var bSelected = false
                var iHours = 0
                try {
                    bSelected = selected.toBoolean()
                    iHours = hour.toInt()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val offer = SDemandOffer(id, code, name, bSelected, title, iHours, co, av, cu, se, un)
                list.add(offer)
            }
        }

        return list
    }
}