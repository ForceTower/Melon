/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
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

package dev.forcetower.conference.core.ui.schedule

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.graphics.withTranslation
import androidx.core.text.inSpans
import androidx.core.view.children
import androidx.core.view.isEmpty
import androidx.recyclerview.widget.RecyclerView
import dev.forcetower.conference.R
import dev.forcetower.conference.core.model.persistence.Session
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ScheduleTimeHeadersDecoration(
    context: Context,
    sessions: List<Session>
) : RecyclerView.ItemDecoration() {
    private val paint: TextPaint
    private val width: Int
    private val padding: Int
    private val timeTextSize: Int
    private val meridiemTextSize: Int
    private val timeFormatter = DateTimeFormatter.ofPattern("H:mm")
    private val hoursString = context.getString(R.string.time_header_decor_hours)

    private val timeTextSizeSpan: AbsoluteSizeSpan
    private val meridiemTextSizeSpan: AbsoluteSizeSpan
    private val boldSpan = StyleSpan(Typeface.BOLD)

    init {
        val attrs = context.obtainStyledAttributes(
            R.style.Widget_UTheme_TimeHeaders,
            R.styleable.TimeHeader
        )
        paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = attrs.getColorOrThrow(R.styleable.TimeHeader_android_textColor)
            try {
                typeface = ResourcesCompat.getFont(
                    context,
                    attrs.getResourceIdOrThrow(R.styleable.TimeHeader_android_fontFamily)
                )
            } catch (_: Exception) {
                // ignore
            }
        }
        width = attrs.getDimensionPixelSizeOrThrow(R.styleable.TimeHeader_android_width)
        padding = attrs.getDimensionPixelSize(R.styleable.TimeHeader_android_padding, 0)
        timeTextSize = attrs.getDimensionPixelSizeOrThrow(R.styleable.TimeHeader_timeTextSize)
        meridiemTextSize =
            attrs.getDimensionPixelSizeOrThrow(R.styleable.TimeHeader_meridiemTextSize)
        attrs.recycle()

        timeTextSizeSpan = AbsoluteSizeSpan(timeTextSize)
        meridiemTextSizeSpan = AbsoluteSizeSpan(meridiemTextSize)
    }

    private val timeSlots: Map<Int, StaticLayout> =
        indexSessionHeaders(sessions).map {
            it.first to createHeader(it.second)
        }.toMap()

    private fun createHeader(startTime: ZonedDateTime): StaticLayout {
        val text = SpannableStringBuilder().apply {
            inSpans(timeTextSizeSpan) {
                append(timeFormatter.format(startTime))
            }
            append(System.lineSeparator())
            inSpans(meridiemTextSizeSpan, boldSpan) {
                append(hoursString)
            }
        }
        return newStaticLayout(text, paint, width, Layout.Alignment.ALIGN_CENTER, 1f, 0f, false)
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (timeSlots.isEmpty() || parent.isEmpty()) return

        val parentPadding = parent.paddingTop

        var earliestPosition = Int.MAX_VALUE
        var previousHeaderPosition = -1
        var previousHasHeader = false
        var earliestChild: View? = null

        parent.children.asIterable().reversed().forEach { child ->
            if (child.y > parent.height || (child.y + child.height) < 0) return@forEach

            val position = parent.getChildAdapterPosition(child)
            if (position < 0) return@forEach

            if (position < earliestPosition) {
                earliestPosition = position
                earliestChild = child
            }

            val header = timeSlots[position]
            if (header != null) {
                drawHeader(c, child, parentPadding, header, child.alpha, previousHasHeader)
                previousHeaderPosition = position
                previousHasHeader = true
            } else {
                previousHasHeader = false
            }
        }

        if (earliestChild != null && earliestPosition != previousHeaderPosition) {
            findHeaderBeforePosition(earliestPosition)?.let { stickyHeader ->
                previousHasHeader = previousHeaderPosition - earliestPosition == 1
                drawHeader(c, earliestChild!!, parentPadding, stickyHeader, 1f, previousHasHeader)
            }
        }
    }

    private fun findHeaderBeforePosition(position: Int): StaticLayout? {
        for (headerPos in timeSlots.keys.reversed()) {
            if (headerPos < position) {
                return timeSlots[headerPos]
            }
        }
        return null
    }

    private fun drawHeader(
        canvas: Canvas,
        child: View,
        parentPadding: Int,
        header: StaticLayout,
        headerAlpha: Float,
        previousHasHeader: Boolean
    ) {
        val childTop = child.y.toInt()
        val childBottom = childTop + child.height
        var top = (childTop + padding).coerceAtLeast(parentPadding)
        if (previousHasHeader) {
            top = top.coerceAtMost(childBottom - header.height - padding)
        }
        paint.alpha = (headerAlpha * 255).toInt()
        canvas.withTranslation(y = top.toFloat()) {
            header.draw(canvas)
        }
    }
}
