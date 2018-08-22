package com.forcetower.sagres.parsers

import org.jsoup.nodes.Document
import timber.log.Timber

object SagresGradesParser {
    @JvmStatic
    fun extractSemesterCodes(document: Document): List<Pair<Long, String>> {
        val list: MutableList<Pair<Long, String>> = ArrayList()
        val semestersValues = document.selectFirst("select[id=\"ctl00_MasterPlaceHolder_ddPeriodosLetivos_ddPeriodosLetivos\"]")
        if (semestersValues != null) {
            val options = semestersValues.select("option")
            for (option in options) {
                val value = option.`val`("value").text().trim()
                val semester = option.text().trim()
                try {
                    val semesterId = value.toLong()
                    val pair = Pair(semesterId, semester)
                    list.add(pair)
                } catch (e: Exception) {
                    Timber.d("Can't parse long: $value")
                }
            }
            return list
        } else {
            //TODO place crashlytics here
            return list
        }
    }

    @JvmStatic
    fun getSelectedSemester(document: Document): Pair<Boolean, Long>? {
        val values = document.select("option[selected=\"selected\"]")
        return if (values.size == 1) {
            val value = values[0].`val`("value").text().trim()
            try {
                val id = value.toLong()
                Pair(true, id)
            } catch (e: Exception) {
                Timber.d("Can't parse long: $value")
                null
            }
        } else {
            val defValue = document.selectFirst("select[id=\"ctl00_MasterPlaceHolder_ddPeriodosLetivos_ddPeriodosLetivos\"]")
            if (defValue != null) {
                val selected = defValue.selectFirst("option[selected=\"selected\"]")
                if (selected != null) {
                    val value = selected.`val`("value").text().trim()
                    try {
                        val id = value.toLong()
                        Timber.d("Successfully found current semester using the alternate way")
                        Pair(false, id)
                    } catch (e: Exception) {
                        Timber.d("Can't parse long: $value")
                    }
                }
            }
            null
        }
    }
}