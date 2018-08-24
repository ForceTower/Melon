/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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