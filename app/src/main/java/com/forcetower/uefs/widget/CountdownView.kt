/*
 * Copyright (c) 2019.
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

package com.forcetower.uefs.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.postDelayed
import com.airbnb.lottie.LottieAnimationView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.util.siecomp.TimeUtils
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

class CountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val root: View = LayoutInflater.from(context).inflate(R.layout.countdown, this, true)
    private var days1 by AnimateDigitDelegate { root.findViewById(R.id.countdown_days_1) }
    private var days2 by AnimateDigitDelegate { root.findViewById(R.id.countdown_days_2) }
    private var hours1 by AnimateDigitDelegate { root.findViewById(R.id.countdown_hours_1) }
    private var hours2 by AnimateDigitDelegate { root.findViewById(R.id.countdown_hours_2) }
    private var mins1 by AnimateDigitDelegate { root.findViewById(R.id.countdown_mins_1) }
    private var mins2 by AnimateDigitDelegate { root.findViewById(R.id.countdown_mins_2) }
    private var secs1 by AnimateDigitDelegate { root.findViewById(R.id.countdown_secs_1) }
    private var secs2 by AnimateDigitDelegate { root.findViewById(R.id.countdown_secs_2) }

    private val updateTime: Runnable = object : Runnable {
        private val conferenceStart = TimeUtils.EventDays.first().start

        override fun run() {
            var timeUntilConf = Duration.between(ZonedDateTime.now(), conferenceStart)

            if (timeUntilConf.isNegative) {
                return
            }

            val days = timeUntilConf.toDays()
            days1 = (days / 10).toInt()
            days2 = (days % 10).toInt()
            timeUntilConf = timeUntilConf.minusDays(days)

            val hours = timeUntilConf.toHours()
            hours1 = (hours / 10).toInt()
            hours2 = (hours % 10).toInt()
            timeUntilConf = timeUntilConf.minusHours(hours)

            val mins = timeUntilConf.toMinutes()
            mins1 = (mins / 10).toInt()
            mins2 = (mins % 10).toInt()
            timeUntilConf = timeUntilConf.minusMinutes(mins)

            val secs = timeUntilConf.seconds
            secs1 = (secs / 10).toInt()
            secs2 = (secs % 10).toInt()

            handler?.postDelayed(this, 1_000L) // Run self every second
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Timber.d("Starting countdown")
        handler?.post(updateTime)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Timber.d("Stopping countdown")
        handler?.removeCallbacks(updateTime)
    }

    /**
     * A delegate who upon receiving a new value, runs animations on a view obtained from
     * [viewProvider]
     */
    private class AnimateDigitDelegate(
        private val viewProvider: () -> LottieAnimationView
    ) : ObservableProperty<Int>(-1) {
        override fun afterChange(property: KProperty<*>, oldValue: Int, newValue: Int) {
            // Sanity check, `newValue` should always be in range [0–9]
            if (newValue < 0 || newValue > 9) {
                Timber.e("Trying to animate to digit: $newValue")
                return
            }

            if (oldValue != newValue) {
                val view = viewProvider()
                if (oldValue != -1) {
                    // Animate out the prev digit i.e play the second half of it's comp
                    view.setAnimation("anim/$oldValue.json")
                    view.setMinAndMaxProgress(0.5f, 1f)
                    // Some issues scheduling & playing 2 * 500ms comps every 1s. Speed up the
                    // outward anim slightly to give us some headroom ¯\_(ツ)_/¯
                    view.speed = 1.1f
                    view.playAnimation()

                    view.postDelayed(500L) {
                        view.setAnimation("anim/$newValue.json")
                        view.setMinAndMaxProgress(0f, 0.5f)
                        view.speed = 1f
                        view.playAnimation()
                    }
                } else {
                    // Initial show, just animate in the desired digit
                    view.setAnimation("anim/$newValue.json")
                    view.setMinAndMaxProgress(0f, 0.5f)
                    view.playAnimation()
                }
            }
        }
    }
}