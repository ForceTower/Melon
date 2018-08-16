package com.forcetower.unes.core.util

import android.content.Context
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.unes.R
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
    @BindingAdapter(value = ["timestamped"])
    fun getTimeStampedDate(view: TextView, time: Long) {
        val context = view.context
        val now = System.currentTimeMillis()
        val diff = now - time

        val oneDay = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)
        val oneHor = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)
        val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        val value = when {
            days > 1L -> {
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val str = format.format(Date(time))
                context.getString(R.string.message_received_date_format, str)
            }
            days == 1L -> {
                val hours = TimeUnit.HOURS.convert(diff - oneDay, TimeUnit.MILLISECONDS)
                val str = days.toString() + "d " + hours.toString() + "h"
                context.getString(R.string.message_received_date_ago_format, str)
            }
            else -> {
                val hours = TimeUnit.HOURS.convert(diff, TimeUnit.MILLISECONDS)
                val minutes = TimeUnit.MINUTES.convert(diff - (hours*oneHor), TimeUnit.MILLISECONDS)
                val str = hours.toString() + "h " + minutes + "min"
                context.getString(R.string.message_received_date_ago_format, str)
            }
        }
        view.text = value
    }
}