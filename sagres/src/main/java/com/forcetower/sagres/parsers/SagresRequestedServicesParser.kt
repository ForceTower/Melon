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