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

import com.forcetower.sagres.database.model.SRequestedService
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object SagresRequestedServicesParser {

    fun extractRequestedServices(document: Document): List<SRequestedService> {
        val root = document.selectFirst("table")
        val table = root?.selectFirst("tbody")
        val elements = table?.children()
        elements ?: return listOf()

        return elements.map { parseService(it) }.filter { it.correct }
    }

    private fun parseService(element: Element): SRequestedService {
        val children = element.children()
        return if (children.size >= 6) {
            val service = children[0].text().trim()
            val date = children[1].text().trim()
            val amount = children[2].text().trim().toIntOrNull() ?: 0
            val situation = children[3].text().trim()
            val value = children[4].text().trim()
            val obs = children[5].text().trim()
            SRequestedService(service, date, amount, situation, value, obs)
        } else {
            SRequestedService(correct = false)
        }
    }
}