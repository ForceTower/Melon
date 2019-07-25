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

import com.forcetower.sagres.utils.ConnectedStates
import com.forcetower.sagres.utils.ValueUtils.toDouble
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber

object SagresBasicParser {

    @JvmStatic
    fun needApproval(document: Document): Boolean {
        var approval: Element? = document.selectFirst("div[class=\"acesso-externo-pagina-aLogin\"]")
        if (approval != null) return true

        approval = document.selectFirst("input[value=\"Acessar o SAGRES Portal\"]")
        return approval != null
    }

    @JvmStatic
    fun isConnected(document: Document?): ConnectedStates {
        if (document == null) return ConnectedStates.UNKNOWN

        val elements = document.select("span[id=\"ctl00_PageContent_lblSessionTimedOut\"]")
        if (elements.isNotEmpty()) {
            return ConnectedStates.SESSION_TIMEOUT
        }

        val element = document.selectFirst("div[class=\"externo-erro\"]")
        return if (element == null) ConnectedStates.CONNECTED else ConnectedStates.INVALID
    }

    @JvmStatic
    fun getScore(document: Document?): Double {
        if (document == null) {
            Timber.d("Document is null. Score will not be parsed")
            return -1.0
        }

        val elements = document.select("div[class=\"situacao-escore\"]")
        if (elements.isEmpty()) Timber.d("<score_404> :: No elements")
        for (element in elements) {
            if (element != null) {
                val score = element.selectFirst("span[class=\"destaque\"]")
                if (score != null) {
                    try {
                        var text = score.text()
                        text = text.replace("[^\\d,]".toRegex(), "")
                        text = text.replace(",", ".")
                        Timber.d("Text at score parsing: $text")
                        val d = toDouble(text, -1.0)
                        if (d != -1.0) return d
                    } catch (ignored: Exception) { }
                } else {
                    Timber.d("Score element is null")
                }
            } else {
                Timber.d("Main Score element is null")
            }
        }
        return -1.0
    }

    @JvmStatic
    fun isDemandOpen(document: Document): Boolean {
        try {
            val elements = document.select("div[class=\"menu-item\"]")
            for (element in elements) {
                val first = element.selectFirst("a[href]")
                if (first != null && first.text().equals("Demanda", ignoreCase = true))
                    return true
            }
        } catch (ignored: Throwable) {}
        return false
    }

    fun getName(document: Document?): String? {
        document ?: return null
        val nameCss = document.selectFirst("span[class=\"usuario-nome\"]")
        return nameCss?.text()?.trim()
    }
}
