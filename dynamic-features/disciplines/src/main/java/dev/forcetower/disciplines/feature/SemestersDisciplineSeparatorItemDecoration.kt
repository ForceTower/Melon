/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package dev.forcetower.disciplines.feature

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.SparseArray
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.graphics.withTranslation
import androidx.core.util.containsKey
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.ui.disciplines.DisciplinesIndexed
import com.forcetower.uefs.feature.shared.extensions.makeSemester
import kotlin.math.ceil

class SemestersDisciplineSeparatorItemDecoration(
    private val context: Context,
    indexed: DisciplinesIndexed
) : RecyclerView.ItemDecoration() {
    private val labels: SparseArray<StaticLayout>
    private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val textWidth: Int
    private val decorHeight: Int
    private val verticalBias: Float

    init {
        val attrs = context.obtainStyledAttributes(
            R.style.Widget_UTheme_SemestersDisciplineSeparatorItemDecoration,
            R.styleable.SemestersDisciplineSeparatorItemDecoration
        )

        val textSize =
            attrs.getDimension(R.styleable.SemestersDisciplineSeparatorItemDecoration_android_textSize, paint.textSize)
        paint.textSize = textSize
        try {
            paint.typeface = ResourcesCompat.getFont(
                context,
                attrs.getResourceIdOrThrow(R.styleable.SemestersDisciplineSeparatorItemDecoration_android_fontFamily)
            )
        } catch (ignored: Exception) {
        }

        val textColor =
            attrs.getColor(R.styleable.SemestersDisciplineSeparatorItemDecoration_android_textColor, Color.BLACK)
        paint.color = textColor

        textWidth =
            attrs.getDimensionPixelSizeOrThrow(R.styleable.SemestersDisciplineSeparatorItemDecoration_android_width)
        val height =
            attrs.getDimensionPixelSizeOrThrow(R.styleable.SemestersDisciplineSeparatorItemDecoration_android_height)
        val minHeight = ceil(textSize).toInt()
        decorHeight = height.coerceAtLeast(minHeight)

        verticalBias = attrs.getFloat(R.styleable.SemestersDisciplineSeparatorItemDecoration_verticalBias, 0.5f)
            .coerceIn(0f, 1f)

        attrs.recycle()

        labels = buildLabels(indexed)
    }

    private fun buildLabels(
        indexer: DisciplinesIndexed
    ): SparseArray<StaticLayout> {
        val sparseArray = SparseArray<StaticLayout>()
        for (semester in indexer.semesters) {
            val position = indexer.positionForSemester(semester)
            val text = context.getString(R.string.semester_discipline_separator, semester.codename.makeSemester())
            val label = newStaticLayout(text, paint, textWidth, Layout.Alignment.ALIGN_CENTER, 1f, 0f, false)
            sparseArray.put(position, label)
        }
        return sparseArray
    }

    override fun getItemOffsets(outRect: Rect, child: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(child)
        outRect.top = if (labels.containsKey(position)) decorHeight else 0
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager ?: return
        val centerX = parent.width / 2f

        parent.forEach { child ->
            if (child.top < parent.height && child.bottom > 0) {
                val layout = labels[parent.getChildAdapterPosition(child)]
                if (layout != null) {
                    val dx = centerX - (layout.width / 2)
                    val dy = layoutManager.getDecoratedTop(child) +
                        child.translationY +
                        (decorHeight - layout.height) * verticalBias
                    canvas.withTranslation(x = dx, y = dy) {
                        layout.draw(this)
                    }
                }
            }
        }
    }

    companion object {
        fun newStaticLayout(
            source: CharSequence,
            paint: TextPaint,
            width: Int,
            alignment: Layout.Alignment,
            spacingmult: Float,
            spacingadd: Float,
            includepad: Boolean
        ): StaticLayout {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(source, 0, source.length, paint, width).apply {
                    setAlignment(alignment)
                    setLineSpacing(spacingadd, spacingmult)
                    setIncludePad(includepad)
                }.build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(source, paint, width, alignment, spacingmult, spacingadd, includepad)
            }
        }
    }
}
