package com.forcetower.unes.core.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {

    /**
     * Different Strings returned based on the difference in days
     *
     * if diff >= 2 return is date, eg 25/06/2020
     * if diff 1..2 return is day + hours, eg 1d 21h
     * if diff 0..1 return is hour + minute, eg 3h 24min
     */
    @JvmStatic
    fun getTimeStampedDate(time: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - time

        val oneDay = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)
        val oneHor = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)
        val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        return when {
            days > 1 -> {
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                format.format(Date(time))
            }
            days == 1L -> {
                val hours = TimeUnit.HOURS.convert(diff - oneDay, TimeUnit.MILLISECONDS)
                days.toString() + "d " + hours + "h atrás"
            }
            else -> {
                val hours = TimeUnit.HOURS.convert(diff, TimeUnit.MILLISECONDS)
                val minutes = TimeUnit.MINUTES.convert(diff - (hours*oneHor), TimeUnit.MILLISECONDS)
                hours.toString() + "h " + minutes + "min atrás"
            }
        }
    }
}