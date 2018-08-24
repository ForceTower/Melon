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

import com.forcetower.sagres.database.model.SLinker
import com.forcetower.sagres.database.model.SMessage

import org.jsoup.nodes.Document

import java.util.ArrayList

/**
 * Created by João Paulo on 06/03/2018.
 */

object SagresMessageParser {
    private const val MESSAGE_CLASS_RECEIVED = "class=\"recado-escopo\">"
    private const val MESSAGE_DATE_RECEIVED = "class=\"recado-data\">"
    private const val MESSAGE_MESSAGE_RECEIVED = "class=\"recado-texto\">"
    private const val MESSAGE_FROM_RECEIVED = "class=\"recado-remetente\">"

    @JvmStatic
    fun getMessages(document: Document): List<SMessage> {
        val html = document.html()
        val messages = ArrayList<SMessage>()

        var position = 0
        var found = html.indexOf("<article id", position) != -1

        while (found) {
            val start = html.indexOf("<article id", position)
            val end = html.indexOf("</article>", start)

            if (start == -1)
                return messages

            found = true

            val article = html.substring(start, end)

            val message = extractInfoArticle(article)
            if (message != null) messages.add(message)
            position = end
        }

        return messages
    }

    private fun extractInfoArticle(article: String): SMessage? {
        val clazz = extractArticleForm1(MESSAGE_CLASS_RECEIVED, article)
        val date = extractArticleForm1(MESSAGE_DATE_RECEIVED, article)
        val message = extractArticleForm2(MESSAGE_MESSAGE_RECEIVED, article)
        val from = extractArticleForm2(MESSAGE_FROM_RECEIVED, article)
        //TODO Figure this out
        return null
    }

    private fun extractArticleForm2(regex: String, article: String): String? {
        val startRRE = article.indexOf(regex)
        if (startRRE != -1) {
            val endRRE = article.indexOf("</span>", startRRE)
            var message = article.substring(startRRE, endRRE).trim { it <= ' ' }
            message = message.substring(regex.length + 1)

            message = message.substring(message.indexOf(">") + 1).trim { it <= ' ' }
            return message
        }

        return null
    }

    private fun extractArticleForm1(regex: String, article: String): String? {
        val startCRE = article.indexOf(regex)
        if (startCRE != -1) {
            val endCRE = article.indexOf("</span>", startCRE)
            var extracted = article.substring(startCRE, endCRE)

            //CLASS NAME
            extracted = extracted.substring(extracted.indexOf(">") + 1).trim { it <= ' ' }
            return extracted
        }

        return null
    }
}
