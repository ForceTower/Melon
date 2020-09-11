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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.graphics.drawable.shapes.Shape
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.appcompat.graphics.drawable.DrawableWrapper
import androidx.appcompat.widget.AppCompatRatingBar

class RatingBarVectorFix @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.ratingBarStyle
) : AppCompatRatingBar(context, attrs, defStyleAttr) {
    private var mSampleTile: Bitmap? = null

    private val drawableShape: Shape
        get() {
            val roundedCorners = floatArrayOf(5f, 5f, 5f, 5f, 5f, 5f, 5f, 5f)
            return RoundRectShape(roundedCorners, null, null)
        }

    override fun setProgressDrawable(d: Drawable) {
        if (isInEditMode) {
            super.setProgressDrawable(d)
            return
        }

        val tiled = tileify(d, false) as LayerDrawable
        super.setProgressDrawable(tiled)
    }

    /**
     * Converts a drawable to a tiled version of itself. It will recursively
     * traverse layer and state list drawables.
     */
    @SuppressLint("RestrictedApi")
    private fun tileify(drawable: Drawable, clip: Boolean): Drawable {
        if (drawable is DrawableWrapper) {
            var inner: Drawable? = drawable.wrappedDrawable
            if (inner != null) {
                inner = tileify(inner, clip)
                drawable.wrappedDrawable = inner
            }
        } else if (drawable is LayerDrawable) {
            val layers = drawable.numberOfLayers
            val outDrawables = arrayOfNulls<Drawable>(layers)

            for (i in 0 until layers) {
                val id = drawable.getId(i)
                outDrawables[i] = tileify(
                    drawable.getDrawable(i),
                    id == android.R.id.progress || id == android.R.id.secondaryProgress
                )
            }
            val newBg = LayerDrawable(outDrawables)

            for (i in 0 until layers) {
                newBg.setId(i, drawable.getId(i))
            }

            return newBg
        } else if (drawable is BitmapDrawable) {
            val tileBitmap = drawable.bitmap
            if (mSampleTile == null) {
                mSampleTile = tileBitmap
            }

            val shapeDrawable = ShapeDrawable(drawableShape)
            val bitmapShader = BitmapShader(
                tileBitmap,
                Shader.TileMode.REPEAT,
                Shader.TileMode.CLAMP
            )
            shapeDrawable.paint.shader = bitmapShader
            shapeDrawable.paint.colorFilter = drawable.paint.colorFilter
            return if (clip)
                ClipDrawable(
                    shapeDrawable,
                    Gravity.START,
                    ClipDrawable.HORIZONTAL
                )
            else
                shapeDrawable
        } else {
            return tileify(getBitmapDrawableFromVectorDrawable(drawable), clip)
        }

        return drawable
    }

    private fun getBitmapDrawableFromVectorDrawable(drawable: Drawable): BitmapDrawable {
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDrawable(resources, bitmap)
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mSampleTile != null) {
            val width = mSampleTile!!.width * numStars
            setMeasuredDimension(
                View.resolveSizeAndState(width, widthMeasureSpec, 0),
                measuredHeight
            )
        }
    }
}
