package dev.forcetower.unes.ui.feature.courseprogress

import android.icu.text.MessageFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Number and date formatting for the progress screens. Everything is locale-
// driven — the same values read "1.500 h" / "53,19%" / "3º" in pt-BR and
// "1,500 h" / "53.19%" / "3rd" in English, with no hardcoded separators.
// Mirrors iOS `CourseProgressFormat`.
internal object CourseProgressFormat {

    // Grouped integer — "4.040".
    fun count(value: Int, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getIntegerInstance(locale).format(value.toLong())

    // `value` is 0…100. Whole numbers print bare ("53%"), the rest keep up to
    // `fractionDigits` ("53,19%").
    fun percent(
        value: Double,
        fractionDigits: Int = 2,
        locale: Locale = Locale.getDefault(),
    ): String = NumberFormat.getPercentInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = fractionDigits
    }.format(value / 100)

    // The headline percent — always one decimal so 37,1% never rounds up to a
    // round 40 in the reader's head.
    fun headlinePercent(value: Double, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getPercentInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.format(value / 100)

    // Ordinal período — "3º" (pt-BR) / "3rd" (en), through ICU's own ordinal
    // rules. Locales ICU has no rules for fall back to the plain number.
    fun ordinal(period: Int, locale: Locale = Locale.getDefault()): String =
        runCatching {
            ordinalFormats.getOrPut(locale) { MessageFormat("{0,ordinal}", locale) }
                .format(arrayOf(period))
        }.getOrNull() ?: period.toString()

    private val ordinalFormats = mutableMapOf<Locale, MessageFormat>()

    // "16 de ago de 2026 07:12" — when the mirror last heard from the portal.
    fun syncedAt(epochMillis: Long, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
            .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    // The source-document date — "4 de mar de 2024". Null when the payload's
    // `asOf` isn't a date we can read.
    fun asOf(raw: String, locale: Locale = Locale.getDefault()): String? =
        runCatching { LocalDate.parse(raw) }.getOrNull()?.let {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(it)
        }
}
