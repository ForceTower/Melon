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

package `in`.uncod.android.bypass

import android.text.SpannableStringBuilder

/**
 * Exactly the same as SpannableStringBuilder, but it returns its spans in reverse.
 *
 *
 * What effect does this have? Well, if you're building up a Spannable recursively (as we
 * are doing in Bypass) then returning the spans in reverse order has the correct effect
 * in some corner cases regarding leading spans.
 *
 *
 * Example:
 * Suppose we have a BLOCK_QUOTE with a LIST inside of it. Both of them have leading spans, but the LIST
 * span is set first. As a result, the QuoteSpan for the BLOCK_QUOTE is actually indented by the LIST's span!
 * If the order is reversed, then the LIST's margin span is properly indented (and the BlockQuote remains on
 * the side).
 */
class ReverseSpannableStringBuilder : SpannableStringBuilder() {
    override fun <T> getSpans(queryStart: Int, queryEnd: Int, kind: Class<T>?): Array<T> {
        val ret = super.getSpans(queryStart, queryEnd, kind)
        return ret.reversedArray()
    }
}
