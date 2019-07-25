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

import com.forcetower.sagres.database.model.SMessage
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.ArrayList
import java.util.Locale

/**
 * Created by João Paulo on 06/03/2018.
 */

object SagresMessageParser {
    private const val MESSAGE_CLASS_RECEIVED = "class=\"recado-escopo\">"
    private const val MESSAGE_DATE_RECEIVED = "class=\"recado-data\">"
    private const val MESSAGE_MESSAGE_RECEIVED = "class=\"recado-texto\">"
    private const val MESSAGE_FROM_RECEIVED = "class=\"recado-remetente\">"

    @JvmStatic
    fun getMessages(html: String): List<SMessage> {
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

    @JvmStatic
    fun getMessages(document: Document): List<SMessage> {
        return getNewMessages(document)
    }

    @JvmStatic
    private fun getNewMessages(document: Document): List<SMessage> {
        val list = mutableListOf<SMessage>()

        val articles = document.select("article")
        articles.forEachIndexed { _, article ->
            val scope = article.selectFirst("span[class=\"recado-escopo\"]")?.text()?.trim()
            val dated = article.selectFirst("span[class=\"recado-data\"]")?.text()?.trim()
            val message = article.selectFirst("p[class=\"recado-texto\"]")
                    ?.wholeText()
                    ?.trim()
                    ?.removePrefix("Descrição do Recado:")
                    ?.trim()

            val (attachmentName, attachmentLink) = article.selectFirst("span[class=\"material_apoio_arquivo\"]")
                    ?.run {
                        val link = selectFirst("a[href]")?.attr("href")
                        val name = parent().children().run {
                            if (size >= 3) {
                                get(1).text().trim()
                            }
                            null
                        }
                        name to link
                    } ?: null to null

            if (attachmentLink != null) Timber.d("Weow! An attachment $attachmentName $attachmentLink")
            val info = article.selectFirst("i[class=\"recado-remetente\"]")?.text()
                    ?.trim()
                    ?.removePrefix("De")
                    ?.trim()

            val information = SMessage(
                message?.toLowerCase(Locale.getDefault()).hashCode().toLong(),
                null,
                null,
                message,
                -2,
                info,
                null,
                attachmentName,
                attachmentLink
            ).apply {
                isFromHtml = true
                discipline = scope
                dateString = dated
                processingTime = System.currentTimeMillis()
            }
            list.add(information)
        }

        return list
    }

    private fun extractInfoArticle(article: String): SMessage? {
        val clazz = extractArticleForm1(MESSAGE_CLASS_RECEIVED, article)
        val date = extractArticleForm1(MESSAGE_DATE_RECEIVED, article)
        val message = extractArticleForm2(MESSAGE_MESSAGE_RECEIVED, article)
        val from = extractArticleForm2(MESSAGE_FROM_RECEIVED, article)
        return SMessage(
            message?.toLowerCase(Locale.getDefault()).hashCode().toLong(),
            null,
            null,
            message,
            -2,
            from,
            null,
            null,
            null
        ).apply {
            isFromHtml = true
            discipline = clazz
            dateString = date
            processingTime = System.currentTimeMillis()
        }
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

            // CLASS NAME
            extracted = extracted.substring(extracted.indexOf(">") + 1).trim { it <= ' ' }
            return extracted
        }

        return null
    }
}
