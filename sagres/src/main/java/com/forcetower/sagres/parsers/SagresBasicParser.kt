/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.sagres.parsers

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
    fun isConnected(document: Document?): Boolean {
        if (document == null) return false

        val element = document.selectFirst("div[class=\"externo-erro\"]")
        return if (element != null) {
            if (element.text().isNotEmpty()) {
                false
            } else {
                false
            }
        } else {
            true
        }
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
}
