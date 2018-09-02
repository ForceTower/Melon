/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package in.uncod.android.bypass;

import android.text.SpannableStringBuilder;

/**
 * Exactly the same as SpannableStringBuilder, but it returns its spans in reverse.
 * <p/>
 * What effect does this have? Well, if you're building up a Spannable recursively (as we
 * are doing in Bypass) then returning the spans in reverse order has the correct effect
 * in some corner cases regarding leading spans.
 * <p/>
 * Example:
 * Suppose we have a BLOCK_QUOTE with a LIST inside of it. Both of them have leading spans, but the LIST
 * span is set first. As a result, the QuoteSpan for the BLOCK_QUOTE is actually indented by the LIST's span!
 * If the order is reversed, then the LIST's margin span is properly indented (and the BlockQuote remains on
 * the side).
 */
public class ReverseSpannableStringBuilder extends SpannableStringBuilder {

    private static void reverse(Object[] arr) {
        if (arr == null) {
            return;
        }

        int i = 0;
        int j = arr.length - 1;
        Object tmp;
        while (j > i) {
            tmp = arr[j];
            arr[j] = arr[i];
            arr[i] = tmp;
            j--;
            i++;
        }
    }

    @Override
    public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind) {
        T[] ret = super.getSpans(queryStart, queryEnd, kind);
        reverse(ret);
        return ret;
    }
}
