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

package com.forcetower.unes.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.FontRes
import androidx.appcompat.widget.AppCompatTextView
import com.forcetower.unes.R

class BaselineGridTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle
): AppCompatTextView(context, attrs, defStyleAttr) {

    private val fourDp: Float
    private var lineHeightMultiplierHint = 1f
    private var lineHeightHint = 0f
    private var maxLinesByHeight = false
    private var extraTopPadding = 0
    private var extraBottomPadding = 0

    @FontRes
    @get:FontRes
    var fontResId = 0
        private set

    init {
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.BaselineGridTextView, defStyleAttr, 0)

        if (a.hasValue(R.styleable.BaselineGridTextView_android_textAppearance)) {
            val textAppearanceId = a.getResourceId(R.styleable.BaselineGridTextView_android_textAppearance,
                    android.R.style.TextAppearance)
            val ta = context.obtainStyledAttributes(
                    textAppearanceId, R.styleable.BaselineGridTextView)
            parseTextAttrs(ta)
            ta.recycle()
        }

        parseTextAttrs(a)
        maxLinesByHeight = a.getBoolean(R.styleable.BaselineGridTextView_maxLinesByHeight, false)
        a.recycle()

        fourDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)
        computeLineHeight()
    }

    fun getLineHeightMultiplierHint(): Float {
        return lineHeightMultiplierHint
    }

    fun setLineHeightMultiplierHint(lineHeightMultiplierHint: Float) {
        this.lineHeightMultiplierHint = lineHeightMultiplierHint
        computeLineHeight()
    }

    fun getLineHeightHint(): Float {
        return lineHeightHint
    }

    fun setLineHeightHint(lineHeightHint: Float) {
        this.lineHeightHint = lineHeightHint
        computeLineHeight()
    }

    fun getMaxLinesByHeight(): Boolean {
        return maxLinesByHeight
    }

    fun setMaxLinesByHeight(maxLinesByHeight: Boolean) {
        this.maxLinesByHeight = maxLinesByHeight
        requestLayout()
    }

    override fun getCompoundPaddingTop(): Int {
        return super.getCompoundPaddingTop() + extraTopPadding
    }

    override fun getCompoundPaddingBottom(): Int {
        return super.getCompoundPaddingBottom() + extraBottomPadding
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        extraBottomPadding = 0
        extraTopPadding = 0
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var height = measuredHeight
        height += ensureBaselineOnGrid()
        height += ensureHeightGridAligned(height)
        setMeasuredDimension(measuredWidth, height)
        checkMaxLines(height, View.MeasureSpec.getMode(heightMeasureSpec))
    }

    private fun parseTextAttrs(a: TypedArray) {
        if (a.hasValue(R.styleable.BaselineGridTextView_lineHeightMultiplierHint)) {
            lineHeightMultiplierHint = a.getFloat(R.styleable.BaselineGridTextView_lineHeightMultiplierHint, 1f)
        }
        if (a.hasValue(R.styleable.BaselineGridTextView_lineHeightHint)) {
            lineHeightHint = a.getDimensionPixelSize(
                    R.styleable.BaselineGridTextView_lineHeightHint, 0).toFloat()
        }
        if (a.hasValue(R.styleable.BaselineGridTextView_android_fontFamily)) {
            fontResId = a.getResourceId(R.styleable.BaselineGridTextView_android_fontFamily, 0)
        }
    }

    private fun computeLineHeight() {
        val fm = paint.fontMetrics
        val fontHeight = Math.abs(fm.ascent - fm.descent) + fm.leading
        val desiredLineHeight = if (lineHeightHint > 0)
            lineHeightHint
        else
            lineHeightMultiplierHint * fontHeight

        val baselineAlignedLineHeight = (fourDp * Math.ceil((desiredLineHeight / fourDp).toDouble()).toFloat() + 0.5f).toInt()
        setLineSpacing(baselineAlignedLineHeight - fontHeight, 1f)
    }

    private fun ensureBaselineOnGrid(): Int {
        val baseline = baseline.toFloat()
        val gridAlign = baseline % fourDp
        if (gridAlign != 0f) {
            extraTopPadding = (fourDp - Math.ceil(gridAlign.toDouble())).toInt()
        }
        return extraTopPadding
    }

    private fun ensureHeightGridAligned(height: Int): Int {
        val gridOverhang = height % fourDp
        if (gridOverhang != 0f) {
            extraBottomPadding = (fourDp - Math.ceil(gridOverhang.toDouble())).toInt()
        }
        return extraBottomPadding
    }

    private fun checkMaxLines(height: Int, heightMode: Int) {
        if (!maxLinesByHeight || heightMode != View.MeasureSpec.EXACTLY) return

        val textHeight = height - compoundPaddingTop - compoundPaddingBottom
        val completeLines = Math.floor((textHeight / lineHeight).toDouble()).toInt()
        maxLines = completeLines
    }
}