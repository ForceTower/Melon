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

package com.forcetower.unes.core.util

import android.animation.Animator
import android.animation.TimeInterpolator
import android.content.Context
import android.os.Build
import android.util.ArrayMap
import android.util.FloatProperty
import android.util.IntProperty
import android.util.Property
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator


object AnimUtils {
    private var fastOutSlowIn: Interpolator? = null
    private var fastOutLinearIn: Interpolator? = null
    private var linearOutSlowIn: Interpolator? = null
    private var linear: Interpolator? = null

    @JvmStatic
    val linearInterpolator: Interpolator
        get() {
            if (linear == null) {
                linear = LinearInterpolator()
            }
            return linear!!
        }

    @JvmStatic
    fun getFastOutSlowInInterpolator(context: Context): Interpolator? {
        if (fastOutSlowIn == null) {
            fastOutSlowIn = AnimationUtils.loadInterpolator(context,
                    android.R.interpolator.fast_out_slow_in)
        }
        return fastOutSlowIn
    }

    @JvmStatic
    fun getFastOutLinearInInterpolator(context: Context): Interpolator? {
        if (fastOutLinearIn == null) {
            fastOutLinearIn = AnimationUtils.loadInterpolator(context,
                    android.R.interpolator.fast_out_linear_in)
        }
        return fastOutLinearIn
    }

    @JvmStatic
    fun getLinearOutSlowInInterpolator(context: Context): Interpolator? {
        if (linearOutSlowIn == null) {
            linearOutSlowIn = AnimationUtils.loadInterpolator(context,
                    android.R.interpolator.linear_out_slow_in)
        }
        return linearOutSlowIn
    }

    /**
     * Linear interpolate between a and b with parameter t.
     */
    @JvmStatic
    fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    /**
     * A delegate for creating a [Property] of `int` type.
     */
    abstract class IntProp<T>(val name: String) {

        abstract operator fun set(`object`: T, value: Int)
        abstract operator fun get(`object`: T): Int
    }

    /**
     * The animation framework has an optimization for `Properties` of type
     * `int` but it was only made public in API24, so wrap the impl in our own type
     * and conditionally create the appropriate type, delegating the implementation.
     */
    @JvmStatic
    fun <T> createIntProperty(impl: IntProp<T>): Property<T, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            object : IntProperty<T>(impl.name) {
                override fun get(`object`: T): Int? {
                    return impl[`object`]
                }

                override fun setValue(`object`: T, value: Int) {
                    impl[`object`] = value
                }
            }
        } else {
            object : Property<T, Int>(Int::class.java, impl.name) {
                override operator fun get(`object`: T): Int? {
                    return impl[`object`]
                }

                override operator fun set(`object`: T, value: Int?) {
                    impl[`object`] = value!!
                }
            }
        }
    }

    /**
     * A delegate for creating a [Property] of `float` type.
     */
    abstract class FloatProp<T> protected constructor(val name: String) {

        abstract operator fun set(`object`: T, value: Float)
        abstract operator fun get(`object`: T): Float
    }

    /**
     * The animation framework has an optimization for `Properties` of type
     * `float` but it was only made public in API24, so wrap the impl in our own type
     * and conditionally create the appropriate type, delegating the implementation.
     */
    @JvmStatic
    fun <T> createFloatProperty(impl: FloatProp<T>): Property<T, Float> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            object : FloatProperty<T>(impl.name) {
                override fun get(`object`: T): Float? {
                    return impl[`object`]
                }

                override fun setValue(`object`: T, value: Float) {
                    impl[`object`] = value
                }
            }
        } else {
            object : Property<T, Float>(Float::class.java, impl.name) {
                override operator fun get(`object`: T): Float? {
                    return impl[`object`]
                }

                override operator fun set(`object`: T, value: Float?) {
                    impl[`object`] = value!!
                }
            }
        }
    }

    /**
     * https://halfthought.wordpress.com/2014/11/07/reveal-transition/
     *
     *
     * Interrupting Activity transitions can yield an OperationNotSupportedException when the
     * transition tries to pause the animator. Yikes! We can fix this by wrapping the Animator:
     */
    class NoPauseAnimator(private val mAnimator: Animator) : Animator() {
        private val mListeners = ArrayMap<AnimatorListener, AnimatorListener>()

        override fun getDuration(): Long= mAnimator.duration

        override fun getInterpolator(): TimeInterpolator = mAnimator.interpolator

        override fun setInterpolator(value: TimeInterpolator?) {
            mAnimator.interpolator = value
        }

        override fun getListeners(): ArrayList<AnimatorListener> = ArrayList(mListeners.keys)

        override fun getStartDelay(): Long = mAnimator.startDelay

        override fun setStartDelay(delayMS: Long) {
            mAnimator.startDelay = delayMS
        }

        override fun isPaused(): Boolean = mAnimator.isPaused

        override fun isRunning(): Boolean = mAnimator.isRunning

        override fun isStarted(): Boolean = mAnimator.isStarted

        override fun addListener(listener: AnimatorListener) {
            val wrapper = AnimatorListenerWrapper(this, listener)
            if (!mListeners.containsKey(listener)) {
                mListeners.put(listener, wrapper)
                mAnimator.addListener(wrapper)
            }
        }

        override fun cancel() {
            mAnimator.cancel()
        }

        override fun end() {
            mAnimator.end()
        }

        override fun removeAllListeners() {
            mListeners.clear()
            mAnimator.removeAllListeners()
        }

        override fun removeListener(listener: AnimatorListener) {
            val wrapper = mListeners.get(listener)
            if (wrapper != null) {
                mListeners.remove(listener)
                mAnimator.removeListener(wrapper)
            }
        }

        override fun setDuration(durationMS: Long): Animator {
            mAnimator.duration = durationMS
            return this
        }

        override fun setTarget(target: Any) {
            mAnimator.setTarget(target)
        }

        override fun setupEndValues() {
            mAnimator.setupEndValues()
        }

        override fun setupStartValues() {
            mAnimator.setupStartValues()
        }

        override fun start() {
            mAnimator.start()
        }
    }

    private class AnimatorListenerWrapper internal constructor(private val mAnimator: Animator, private val mListener: Animator.AnimatorListener) : Animator.AnimatorListener {

        override fun onAnimationStart(animator: Animator) {
            mListener.onAnimationStart(mAnimator)
        }

        override fun onAnimationEnd(animator: Animator) {
            mListener.onAnimationEnd(mAnimator)
        }

        override fun onAnimationCancel(animator: Animator) {
            mListener.onAnimationCancel(mAnimator)
        }

        override fun onAnimationRepeat(animator: Animator) {
            mListener.onAnimationRepeat(mAnimator)
        }
    }

}
