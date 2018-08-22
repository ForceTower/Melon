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
        if (document == null) return -1.0

        val elements = document.select("div[class=\"situacao-escore\"]")
        for (element in elements) {
            if (element != null) {
                val score = element.selectFirst("span[class=\"destaque\"]")
                if (score != null) {
                    try {
                        var text = score.text()
                        text = text.replace("[^\\d,]".toRegex(), "")
                        text = text.replace(",", ".")
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
