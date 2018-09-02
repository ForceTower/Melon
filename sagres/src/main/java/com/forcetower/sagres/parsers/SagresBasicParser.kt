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
