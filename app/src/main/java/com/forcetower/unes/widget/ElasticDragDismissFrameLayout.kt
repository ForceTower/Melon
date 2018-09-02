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

import android.app.Activity
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

import com.forcetower.unes.R
import com.forcetower.unes.core.util.AnimUtils
import com.forcetower.unes.core.util.ColorUtils
import com.forcetower.unes.core.util.ViewUtils
import com.forcetower.unes.feature.shared.isNavBarOnBottom

import java.util.ArrayList

class ElasticDragDismissFrameLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                              defStyleAttr: Int = 0, defStyleRes: Int = 0) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    // configurable attribs
    private var dragDismissDistance = java.lang.Float.MAX_VALUE
    private var dragDismissFraction = -1f
    private var dragDismissScale = 1f
    private var shouldScale = false
    private var dragElacticity = 0.8f

    // state
    private var totalDrag: Float = 0.toFloat()
    private var draggingDown = false
    private var draggingUp = false

    private var callbacks: MutableList<ElasticDragDismissCallback>? = null

    init {

        val a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ElasticDragDismissFrameLayout, 0, 0)

        if (a.hasValue(R.styleable.ElasticDragDismissFrameLayout_dragDismissDistance)) {
            dragDismissDistance = a.getDimensionPixelSize(R.styleable
                    .ElasticDragDismissFrameLayout_dragDismissDistance, 0).toFloat()
        } else if (a.hasValue(R.styleable.ElasticDragDismissFrameLayout_dragDismissFraction)) {
            dragDismissFraction = a.getFloat(R.styleable
                    .ElasticDragDismissFrameLayout_dragDismissFraction, dragDismissFraction)
        }
        if (a.hasValue(R.styleable.ElasticDragDismissFrameLayout_dragDismissScale)) {
            dragDismissScale = a.getFloat(R.styleable
                    .ElasticDragDismissFrameLayout_dragDismissScale, dragDismissScale)
            shouldScale = dragDismissScale != 1f
        }
        if (a.hasValue(R.styleable.ElasticDragDismissFrameLayout_dragElasticity)) {
            dragElacticity = a.getFloat(R.styleable.ElasticDragDismissFrameLayout_dragElasticity,
                    dragElacticity)
        }
        a.recycle()
    }

    abstract class ElasticDragDismissCallback {

        /**
         * Called for each drag event.
         *
         * @param elasticOffset       Indicating the drag offset with elasticity applied i.e. may
         * exceed 1.
         * @param elasticOffsetPixels The elastically scaled drag distance in pixels.
         * @param rawOffset           Value from [0, 1] indicating the raw drag offset i.e.
         * without elasticity applied. A value of 1 indicates that the
         * dismiss distance has been reached.
         * @param rawOffsetPixels     The raw distance the user has dragged
         */
        internal open fun onDrag(elasticOffset: Float, elasticOffsetPixels: Float,
                                 rawOffset: Float, rawOffsetPixels: Float) {
        }

        /**
         * Called when dragging is released and has exceeded the threshold dismiss distance.
         */
        internal open fun onDragDismissed() {}

    }

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return nestedScrollAxes and View.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        // if we're in a drag gesture and the user reverses up the we should take those events
        if (draggingDown && dy > 0 || draggingUp && dy < 0) {
            dragScale(dy)
            consumed[1] = dy
        }
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int,
                                dxUnconsumed: Int, dyUnconsumed: Int) {
        dragScale(dyUnconsumed)
    }

    override fun onStopNestedScroll(child: View) {
        if (Math.abs(totalDrag) >= dragDismissDistance) {
            dispatchDismissCallback()
        } else { // settle back to natural position
            animate()
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200L)
                    .setInterpolator(AnimUtils.getFastOutSlowInInterpolator(context))
                    .setListener(null)
                    .start()
            totalDrag = 0f
            draggingUp = false
            draggingDown = draggingUp
            dispatchDragCallback(0f, 0f, 0f, 0f)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (dragDismissFraction > 0f) {
            dragDismissDistance = h * dragDismissFraction
        }
    }

    fun addListener(listener: ElasticDragDismissCallback) {
        if (callbacks == null) {
            callbacks = ArrayList()
        }
        callbacks!!.add(listener)
    }

    fun removeListener(listener: ElasticDragDismissCallback) {
        if (callbacks != null && callbacks!!.size > 0) {
            callbacks!!.remove(listener)
        }
    }

    private fun dragScale(scroll: Int) {
        if (scroll == 0) return

        totalDrag += scroll.toFloat()

        // track the direction & set the pivot point for scaling
        // don't double track i.e. if start dragging down and then reverse, keep tracking as
        // dragging down until they reach the 'natural' position
        if (scroll < 0 && !draggingUp && !draggingDown) {
            draggingDown = true
            if (shouldScale) pivotY = height.toFloat()
        } else if (scroll > 0 && !draggingDown && !draggingUp) {
            draggingUp = true
            if (shouldScale) pivotY = 0f
        }
        // how far have we dragged relative to the distance to perform a dismiss
        // (0–1 where 1 = dismiss distance). Decreasing logarithmically as we approach the limit
        var dragFraction = Math.log10((1 + Math.abs(totalDrag) / dragDismissDistance).toDouble()).toFloat()

        // calculate the desired translation given the drag fraction
        var dragTo = dragFraction * dragDismissDistance * dragElacticity

        if (draggingUp) {
            // as we use the absolute magnitude when calculating the drag fraction, need to
            // re-apply the drag direction
            dragTo *= -1f
        }
        translationY = dragTo

        if (shouldScale) {
            val scale = 1 - (1 - dragDismissScale) * dragFraction
            scaleX = scale
            scaleY = scale
        }

        // if we've reversed direction and gone past the settle point then clear the flags to
        // allow the list to get the scroll events & reset any transforms
        if (draggingDown && totalDrag >= 0 || draggingUp && totalDrag <= 0) {
            dragFraction = 0f
            dragTo = dragFraction
            totalDrag = dragTo
            draggingUp = false
            draggingDown = draggingUp
            translationY = 0f
            scaleX = 1f
            scaleY = 1f
        }
        dispatchDragCallback(dragFraction, dragTo,
                Math.min(1f, Math.abs(totalDrag) / dragDismissDistance), totalDrag)
    }

    private fun dispatchDragCallback(elasticOffset: Float, elasticOffsetPixels: Float,
                                     rawOffset: Float, rawOffsetPixels: Float) {
        if (callbacks != null && !callbacks!!.isEmpty()) {
            for (callback in callbacks!!) {
                callback.onDrag(elasticOffset, elasticOffsetPixels,
                        rawOffset, rawOffsetPixels)
            }
        }
    }

    private fun dispatchDismissCallback() {
        if (callbacks != null && !callbacks!!.isEmpty()) {
            for (callback in callbacks!!) {
                callback.onDragDismissed()
            }
        }
    }

    /**
     * An [ElasticDragDismissCallback] which fades system chrome (i.e. status bar and
     * navigation bar) whilst elastic drags are performed and
     * [finishes][Activity.finishAfterTransition] the activity when drag dismissed.
     */
    class SystemChromeFader(private val activity: Activity) : ElasticDragDismissCallback() {
        private val statusBarAlpha: Int = Color.alpha(activity.window.statusBarColor)
        private val navBarAlpha: Int = Color.alpha(activity.window.navigationBarColor)
        private val fadeNavBar: Boolean = activity.isNavBarOnBottom()

        public override fun onDrag(elasticOffset: Float, elasticOffsetPixels: Float,
                                   rawOffset: Float, rawOffsetPixels: Float) {
            if (elasticOffsetPixels > 0) {
                // dragging downward, fade the status bar in proportion
                activity.window.statusBarColor = ColorUtils.modifyAlpha(activity.window
                        .statusBarColor, ((1f - rawOffset) * statusBarAlpha).toInt())
            } else if (elasticOffsetPixels == 0f) {
                // reset
                activity.window.statusBarColor = ColorUtils.modifyAlpha(
                        activity.window.statusBarColor, statusBarAlpha)
                activity.window.navigationBarColor = ColorUtils.modifyAlpha(
                        activity.window.navigationBarColor, navBarAlpha)
            } else if (fadeNavBar) {
                // dragging upward, fade the navigation bar in proportion
                activity.window.navigationBarColor = ColorUtils.modifyAlpha(activity.window.navigationBarColor,
                        ((1f - rawOffset) * navBarAlpha).toInt())
            }
        }

        public override fun onDragDismissed() {
            activity.finishAfterTransition()
        }
    }

}