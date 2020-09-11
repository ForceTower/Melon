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

package com.forcetower.uefs.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.FontRes
import androidx.appcompat.widget.AppCompatTextView
import com.forcetower.uefs.R
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

class BaselineGridTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

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
            attrs,
            R.styleable.BaselineGridTextView,
            defStyleAttr,
            0
        )

        if (a.hasValue(R.styleable.BaselineGridTextView_android_textAppearance)) {
            val textAppearanceId = a.getResourceId(
                R.styleable.BaselineGridTextView_android_textAppearance,
                android.R.style.TextAppearance
            )
            val ta = context.obtainStyledAttributes(
                textAppearanceId,
                R.styleable.BaselineGridTextView
            )
            parseTextAttrs(ta)
            ta.recycle()
        }

        parseTextAttrs(a)
        maxLinesByHeight = a.getBoolean(R.styleable.BaselineGridTextView_maxLinesByHeight, false)
        a.recycle()

        fourDp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            resources.displayMetrics
        )
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
        checkMaxLines(height, MeasureSpec.getMode(heightMeasureSpec))
    }

    private fun parseTextAttrs(a: TypedArray) {
        if (a.hasValue(R.styleable.BaselineGridTextView_lineHeightMultiplierHint)) {
            lineHeightMultiplierHint = a.getFloat(R.styleable.BaselineGridTextView_lineHeightMultiplierHint, 1f)
        }
        if (a.hasValue(R.styleable.BaselineGridTextView_lineHeightHint)) {
            lineHeightHint = a.getDimensionPixelSize(
                R.styleable.BaselineGridTextView_lineHeightHint,
                0
            ).toFloat()
        }
        if (a.hasValue(R.styleable.BaselineGridTextView_android_fontFamily)) {
            fontResId = a.getResourceId(R.styleable.BaselineGridTextView_android_fontFamily, 0)
        }
    }

    private fun computeLineHeight() {
        val fm = paint.fontMetrics
        val fontHeight = abs(fm.ascent - fm.descent) + fm.leading
        val desiredLineHeight = if (lineHeightHint > 0)
            lineHeightHint
        else
            lineHeightMultiplierHint * fontHeight

        val baselineAlignedLineHeight = (fourDp * ceil((desiredLineHeight / fourDp).toDouble()).toFloat() + 0.5f).toInt()
        setLineSpacing(baselineAlignedLineHeight - fontHeight, 1f)
    }

    private fun ensureBaselineOnGrid(): Int {
        val baseline = baseline.toFloat()
        val gridAlign = baseline % fourDp
        if (gridAlign != 0f) {
            extraTopPadding = (fourDp - ceil(gridAlign.toDouble())).toInt()
        }
        return extraTopPadding
    }

    private fun ensureHeightGridAligned(height: Int): Int {
        val gridOverhang = height % fourDp
        if (gridOverhang != 0f) {
            extraBottomPadding = (fourDp - ceil(gridOverhang.toDouble())).toInt()
        }
        return extraBottomPadding
    }

    private fun checkMaxLines(height: Int, heightMode: Int) {
        if (!maxLinesByHeight || heightMode != MeasureSpec.EXACTLY) return

        val textHeight = height - compoundPaddingTop - compoundPaddingBottom
        val completeLines = floor((textHeight / lineHeight).toDouble()).toInt()
        maxLines = completeLines
    }
}
