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

import com.forcetower.sagres.database.model.SDemandOffer
import org.jsoup.nodes.Document
import timber.log.Timber

object SagresDemandParser {
    @JvmStatic
    fun getOffers(document: Document): List<SDemandOffer>? {
        val list = mutableListOf<SDemandOffer>()

        // Each element represents a column full of disciplines for the semester
        val elements = document.select("div[class=\"demanda-coluna\"]")
        // If no elements is found, the demand is not available
        if (elements.size == 0) return null

        // We iterate over each "semester"
        for (element in elements) {
            // Get the semester title
            val title = element.selectFirst("span").text().trim()

            // Find the objects that represents the disciplines
            val disciplines = element.select("div[qtcredito][nomeatividade][codigoatividade][qthoras]")
            // And iterate over it
            for (discipline in disciplines) {
                // Extract the attributes
                val id = discipline.selectFirst("input[type=\"hidden\"]").attr("name").trim()
                val selected = discipline.selectFirst("input[type=\"hidden\"]").attr("value").trim()
                val name = discipline.attr("nomeatividade").trim()
                val code = discipline.attr("codigoatividade").trim()
                // val activityId = discipline.attr("idatividade").trim()
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

                // Adds it to the offers
                val offer = SDemandOffer(0, id, code, name, bSelected, title, iHours, co, av, cu, se, un)
                list.add(offer)
            }
        }

        Timber.d("Total list size: ${list.size}")
        return list
    }
}