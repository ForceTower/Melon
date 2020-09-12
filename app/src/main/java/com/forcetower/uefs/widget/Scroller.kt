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
import android.hardware.SensorManager
import android.view.ViewConfiguration
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * This class encapsulates scrolling.  The duration of the scroll
 * can be passed in the constructor and specifies the maximum time that
 * the scrolling animation should take.  Past this time, the scrolling is
 * automatically moved to its final stage and computeScrollOffset()
 * will always return false to indicate that scrolling is over.
 */
class Scroller
/**
 * Create a Scroller with the specified interpolator. If the interpolator is
 * null, the default (viscous) interpolator will be used. Specify whether or
 * not to support progressive "flywheel" behavior in flinging.
 */
@JvmOverloads constructor(context: Context, private val mInterpolator: Interpolator? = null, private val mFlywheel: Boolean = true) {
    private var mMode: Int = 0

    /**
     * Returns the start X offset in the scroll.
     *
     * @return The start X offset as an absolute distance from the origin.
     */
    private var startX: Int = 0
    /**
     * Returns the start Y offset in the scroll.
     *
     * @return The start Y offset as an absolute distance from the origin.
     */
    var startY: Int = 0
        private set
    private var mFinalX: Int = 0
    private var mFinalY: Int = 0

    private var mMinX: Int = 0
    private var mMaxX: Int = 0
    private var mMinY: Int = 0
    private var mMaxY: Int = 0

    /**
     * Returns the current X offset in the scroll.
     *
     * @return The new X offset as an absolute distance from the origin.
     */
    private var currX: Int = 0
    /**
     * Returns the current Y offset in the scroll.
     *
     * @return The new Y offset as an absolute distance from the origin.
     */
    var currY: Int = 0
        private set
    private var mStartTime: Long = 0
    /**
     * Returns how long the scroll event will take, in milliseconds.
     *
     * @return The duration of the scroll in milliseconds.
     */
    var duration: Int = 0
        private set
    private var mDurationReciprocal: Float = 0.toFloat()
    private var mDeltaX: Float = 0.toFloat()
    private var mDeltaY: Float = 0.toFloat()
    /**
     *
     * Returns whether the scroller has finished scrolling.
     *
     * @return True if the scroller has finished scrolling, false otherwise.
     */
    var isFinished: Boolean = false
        private set

    private var mVelocity: Float = 0.toFloat()

    private var mDeceleration: Float = 0.toFloat()
    private val mPpi: Float

    /**
     * Returns the current velocity.
     *
     * @return The original velocity less the deceleration. Result may be
     * negative.
     */
    private val currVelocity: Float
        get() = mVelocity - mDeceleration * timePassed() / 2000.0f

    /**
     * Returns where the scroll will end. Valid only for "fling" scrolls.
     *
     * @return The final X offset as an absolute distance from the origin.
     */
    /**
     * Sets the final position (X) for this scroller.
     *
     * @param newX The new X offset as an absolute distance from the origin.
     * @see .extendDuration
     * @see .setFinalY
     */
    var finalX: Int
        get() = mFinalX
        set(newX) {
            mFinalX = newX
            mDeltaX = (mFinalX - startX).toFloat()
            isFinished = false
        }

    /**
     * Returns where the scroll will end. Valid only for "fling" scrolls.
     *
     * @return The final Y offset as an absolute distance from the origin.
     */
    /**
     * Sets the final position (Y) for this scroller.
     *
     * @param newY The new Y offset as an absolute distance from the origin.
     * @see .extendDuration
     * @see .setFinalX
     */
    var finalY: Int
        get() = mFinalY
        set(newY) {
            mFinalY = newY
            mDeltaY = (mFinalY - startY).toFloat()
            isFinished = false
        }

    init {
        isFinished = true
        mPpi = context.resources.displayMetrics.density * 160.0f
        mDeceleration = computeDeceleration(ViewConfiguration.getScrollFriction())
    }

    /**
     * The amount of friction applied to flings. The default value
     * is [android.view.ViewConfiguration.getScrollFriction].
     *
     * @param friction A scalar dimension-less value representing the coefficient of
     * friction.
     */
    fun setFriction(friction: Float) {
        mDeceleration = computeDeceleration(friction)
    }

    private fun computeDeceleration(friction: Float): Float {
        return (
            SensorManager.GRAVITY_EARTH // g (m/s^2)

                * 39.37f * // inch/meter

                mPpi // pixels per inch

                * friction
            )
    }

    /**
     * Force the finished field to a particular value.
     *
     * @param finished The new finished value.
     */
    fun forceFinished(finished: Boolean) {
        isFinished = finished
    }

    /**
     * Call this when you want to know the new location.  If it returns true,
     * the animation is not yet finished.  loc will be altered to provide the
     * new location.
     */
    fun computeScrollOffset(): Boolean {
        if (isFinished) {
            return false
        }

        val timePassed = (AnimationUtils.currentAnimationTimeMillis() - mStartTime).toInt()

        if (timePassed < duration) {
            when (mMode) {
                SCROLL_MODE -> {
                    var x = timePassed * mDurationReciprocal

                    x = if (mInterpolator == null)
                        viscousFluid(x)
                    else
                        mInterpolator.getInterpolation(x)

                    currX = startX + (x * mDeltaX).roundToInt()
                    currY = startY + (x * mDeltaY).roundToInt()
                }
                FLING_MODE -> {
                    val t = timePassed.toFloat() / duration
                    val index = (NB_SAMPLES * t).toInt()
                    val t_inf = index.toFloat() / NB_SAMPLES
                    val t_sup = (index + 1).toFloat() / NB_SAMPLES
                    val d_inf = SPLINE[index]
                    val d_sup = SPLINE[index + 1]
                    val distanceCoef = d_inf + (t - t_inf) / (t_sup - t_inf) * (d_sup - d_inf)

                    currX = startX + (distanceCoef * (mFinalX - startX)).roundToInt()
                    // Pin to mMinX <= mCurrX <= mMaxX
                    currX = min(currX, mMaxX)
                    currX = max(currX, mMinX)

                    currY = startY + (distanceCoef * (mFinalY - startY)).roundToInt()
                    // Pin to mMinY <= mCurrY <= mMaxY
                    currY = min(currY, mMaxY)
                    currY = max(currY, mMinY)

                    if (currX == mFinalX && currY == mFinalY) {
                        isFinished = true
                    }
                }
            }
        } else {
            currX = mFinalX
            currY = mFinalY
            isFinished = true
        }
        return true
    }

    /**
     * Start scrolling by providing a starting point and the distance to travel.
     *
     * @param startX Starting horizontal scroll offset in pixels. Positive
     * numbers will scroll the content to the left.
     * @param startY Starting vertical scroll offset in pixels. Positive numbers
     * will scroll the content up.
     * @param dx Horizontal distance to travel. Positive numbers will scroll the
     * content to the left.
     * @param dy Vertical distance to travel. Positive numbers will scroll the
     * content up.
     * @param duration Duration of the scroll in milliseconds.
     */
    @JvmOverloads
    fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int = DEFAULT_DURATION) {
        mMode = SCROLL_MODE
        isFinished = false
        this.duration = duration
        mStartTime = AnimationUtils.currentAnimationTimeMillis()
        this.startX = startX
        this.startY = startY
        mFinalX = startX + dx
        mFinalY = startY + dy
        mDeltaX = dx.toFloat()
        mDeltaY = dy.toFloat()
        mDurationReciprocal = 1.0f / this.duration.toFloat()
    }

    /**
     * Start scrolling based on a fling gesture. The distance travelled will
     * depend on the initial velocity of the fling.
     *
     * @param startX Starting point of the scroll (X)
     * @param startY Starting point of the scroll (Y)
     * @param velocityX Initial velocity of the fling (X) measured in pixels per
     * second.
     * @param velocityY Initial velocity of the fling (Y) measured in pixels per
     * second
     * @param minX Minimum X value. The scroller will not scroll past this
     * point.
     * @param maxX Maximum X value. The scroller will not scroll past this
     * point.
     * @param minY Minimum Y value. The scroller will not scroll past this
     * point.
     * @param maxY Maximum Y value. The scroller will not scroll past this
     * point.
     */
    fun fling(
        startX: Int,
        startY: Int,
        tVelocityX: Int,
        tVelocityY: Int,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int
    ) {
        var velocityX = tVelocityX
        var velocityY = tVelocityY
        // Continue a scroll or fling in progress
        if (mFlywheel && !isFinished) {
            val oldVel = currVelocity

            val dx = (mFinalX - this.startX).toFloat()
            val dy = (mFinalY - this.startY).toFloat()
            val hyp = sqrt(dx * dx + dy * dy)

            val ndx = dx / hyp
            val ndy = dy / hyp

            val oldVelocityX = ndx * oldVel
            val oldVelocityY = ndy * oldVel
            if (sign(velocityX.toFloat()) == sign(oldVelocityX) && sign(velocityY.toFloat()) == sign(oldVelocityY)) {
                velocityX += oldVelocityX.toInt()
                velocityY += oldVelocityY.toInt()
            }
        }

        mMode = FLING_MODE
        isFinished = false

        val velocity = sqrt(velocityX * velocityX + velocityY * velocityY.toDouble()).toFloat()

        mVelocity = velocity
        val l = ln((START_TENSION * velocity / ALPHA).toDouble())
        duration = (1000.0 * exp(l / (DECELERATION_RATE - 1.0))).toInt()
        mStartTime = AnimationUtils.currentAnimationTimeMillis()
        this.startX = startX
        this.startY = startY

        val coeffX = if (velocity == 0f) 1.0f else velocityX / velocity
        val coeffY = if (velocity == 0f) 1.0f else velocityY / velocity

        val totalDistance = (ALPHA * exp(DECELERATION_RATE / (DECELERATION_RATE - 1.0) * l)).toInt()

        mMinX = minX
        mMaxX = maxX
        mMinY = minY
        mMaxY = maxY

        mFinalX = startX + (totalDistance * coeffX).roundToInt()
        // Pin to mMinX <= mFinalX <= mMaxX
        mFinalX = min(mFinalX, mMaxX)
        mFinalX = max(mFinalX, mMinX)

        mFinalY = startY + (totalDistance * coeffY).roundToInt()
        // Pin to mMinY <= mFinalY <= mMaxY
        mFinalY = min(mFinalY, mMaxY)
        mFinalY = max(mFinalY, mMinY)
    }

    /**
     * Stops the animation. Contrary to [.forceFinished],
     * aborting the animating cause the scroller to move to the final x and y
     * position
     *
     * @see .forceFinished
     */
    fun abortAnimation() {
        currX = mFinalX
        currY = mFinalY
        isFinished = true
    }

    /**
     * Extend the scroll animation. This allows a running animation to scroll
     * further and longer, when used with [.setFinalX] or [.setFinalY].
     *
     * @param extend Additional time to scroll in milliseconds.
     * @see .setFinalX
     * @see .setFinalY
     */
    fun extendDuration(extend: Int) {
        val passed = timePassed()
        duration = passed + extend
        mDurationReciprocal = 1.0f / duration
        isFinished = false
    }

    /**
     * Returns the time elapsed since the beginning of the scrolling.
     *
     * @return The elapsed time in milliseconds.
     */
    private fun timePassed(): Int {
        return (AnimationUtils.currentAnimationTimeMillis() - mStartTime).toInt()
    }

    /**
     * @hide
     */
    fun isScrollingInDirection(xvel: Float, yvel: Float): Boolean {
        return !isFinished && sign(xvel) == sign((mFinalX - startX).toFloat()) &&
            sign(yvel) == sign((mFinalY - startY).toFloat())
    }

    companion object {
        private var sViscousFluidScale: Float = 0.toFloat()
        private var sViscousFluidNormalize: Float = 0.toFloat()
        private const val DEFAULT_DURATION = 250
        private const val SCROLL_MODE = 0
        private const val FLING_MODE = 1

        private val DECELERATION_RATE = (ln(0.75) / ln(0.9)).toFloat()
        private const val ALPHA = 800f // pixels / seconds
        private const val START_TENSION = 0.4f // Tension at start: (0.4 * total T, 1.0 * Distance)
        private const val END_TENSION = 1.0f - START_TENSION
        private const val NB_SAMPLES = 100
        private val SPLINE = FloatArray(NB_SAMPLES + 1)

        init {
            var x_min = 0.0f
            for (i in 0..NB_SAMPLES) {
                val t = i.toFloat() / NB_SAMPLES
                var x_max = 1.0f
                var x: Float
                var tx: Float
                var coef: Float
                while (true) {
                    x = x_min + (x_max - x_min) / 2.0f
                    coef = 3.0f * x * (1.0f - x)
                    tx = coef * ((1.0f - x) * START_TENSION + x * END_TENSION) + x * x * x
                    if (abs(tx - t) < 1E-5) break
                    if (tx > t)
                        x_max = x
                    else
                        x_min = x
                }
                val d = coef + x * x * x
                SPLINE[i] = d
            }
            SPLINE[NB_SAMPLES] = 1.0f

            // This controls the viscous fluid effect (how much of it)
            sViscousFluidScale = 8.0f
            // must be set to 1.0 (used in viscousFluid())
            sViscousFluidNormalize = 1.0f
            sViscousFluidNormalize = 1.0f / viscousFluid(1.0f)
        }

        internal fun viscousFluid(tX: Float): Float {
            var x = tX
            x *= sViscousFluidScale
            if (x < 1.0f) {
                x -= 1.0f - exp((-x).toDouble()).toFloat()
            } else {
                val start = 0.36787944117f // 1/e == exp(-1)
                x = 1.0f - exp((1.0f - x).toDouble()).toFloat()
                x = start + x * (1.0f - start)
            }
            x *= sViscousFluidNormalize
            return x
        }
    }
}
/**
 * Create a Scroller with the default duration and interpolator.
 */
/**
 * Create a Scroller with the specified interpolator. If the interpolator is
 * null, the default (viscous) interpolator will be used. "Flywheel" behavior will
 * be in effect for apps targeting Honeycomb or newer.
 */
/**
 * Start scrolling by providing a starting point and the distance to travel.
 * The scroll will use the default value of 250 milliseconds for the
 * duration.
 *
 * @param startX Starting horizontal scroll offset in pixels. Positive
 * numbers will scroll the content to the left.
 * @param startY Starting vertical scroll offset in pixels. Positive numbers
 * will scroll the content up.
 * @param dx Horizontal distance to travel. Positive numbers will scroll the
 * content to the left.
 * @param dy Vertical distance to travel. Positive numbers will scroll the
 * content up.
 */
