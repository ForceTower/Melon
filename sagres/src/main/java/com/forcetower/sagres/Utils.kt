package com.forcetower.sagres

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import java.nio.charset.Charset

object Utils {
    @JvmStatic
    fun createDocument(string: String): Document {
        val document = Jsoup.parse(string)
        document.charset(Charset.forName("ISO-8859-1"))
        return document
    }
}
