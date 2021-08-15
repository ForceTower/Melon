package dev.forcetower.noct.design.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class SeamlessCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private var foregroundStrokeWidth = 3f
    private val rectF = RectF()
    private val ovalRect = RectF()
    private val referenceRect = RectF()
    private val interactions = 5

    private var content: Int = 0
    private var halfContent: Int = 0

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3")
        style = Paint.Style.STROKE
        strokeWidth = foregroundStrokeWidth
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3")
        style = Paint.Style.FILL
        strokeWidth = foregroundStrokeWidth
    }

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val min = min(height, width)

        content = (min * 0.08).toInt()
        halfContent = content / 2
        val size = max(min, content)

        setMeasuredDimension(size, size)
        val fl = foregroundStrokeWidth / 2
        rectF.set(fl + content, fl + content, min - fl - content, min - fl - content)
        ovalRect.set(fl + halfContent, fl + halfContent, min - fl - halfContent, min - fl - halfContent)
        referenceRect.set(ovalRect)

        textPaint.apply {
            textSize = size * 0.55f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (i in 1..interactions) {
            val offsetX = ((Random.nextFloat() * 2) - 1) * halfContent
            val offsetY = ((Random.nextFloat() * 2) - 1) * halfContent
            ovalRect.set(referenceRect)
            ovalRect.offset(offsetX, offsetY)
            canvas.drawOval(ovalRect, backgroundPaint)
        }

        canvas.drawOval(rectF, centerPaint)

        val textMeasure = textPaint.measureText("U") / 2
        canvas.drawText("U", referenceRect.centerX() - textMeasure, referenceRect.centerY() + textMeasure, textPaint)
    }
}