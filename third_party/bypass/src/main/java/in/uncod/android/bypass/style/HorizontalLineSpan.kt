package `in`.uncod.android.bypass.style

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

/**
 * Draws a line across the screen.
 */
class HorizontalLineSpan(color: Int, private val mLineHeight: Int, private val mTopBottomPadding: Int) : ReplacementSpan() {
    private val mPaint: Paint = Paint()

    init {
        mPaint.color = color
    }

    override fun getSize(
            paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        if (fm != null) {
            fm.ascent = -mLineHeight - mTopBottomPadding
            fm.descent = 0

            fm.top = fm.ascent
            fm.bottom = 0
        }

        // Take up *all* the horizontal space
        return Integer.MAX_VALUE
    }

    override fun draw(
            canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val middle = (top + bottom) / 2
        val halfLineHeight = mLineHeight / 2
        canvas.drawRect(x, (middle - halfLineHeight).toFloat(), Integer.MAX_VALUE.toFloat(), (middle + halfLineHeight).toFloat(), mPaint)
    }
}
