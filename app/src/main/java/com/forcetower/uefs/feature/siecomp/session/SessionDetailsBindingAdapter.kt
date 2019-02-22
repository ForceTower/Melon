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

package com.forcetower.uefs.feature.siecomp.session

import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.siecomp.Session
import com.forcetower.uefs.core.model.siecomp.SessionType
import com.forcetower.uefs.core.util.siecomp.TimeUtils
import com.forcetower.uefs.widget.HeaderGridDrawable
import org.threeten.bp.Duration
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

@BindingAdapter("headerImage")
fun headerImage(imageView: ImageView, photoUrl: String?) {
    if (!photoUrl.isNullOrEmpty()) {
        Glide.with(imageView)
                .load(photoUrl)
                .apply(RequestOptions().placeholder(HeaderGridDrawable(imageView.context)))
                .into(imageView)
    } else {
        imageView.setImageDrawable(HeaderGridDrawable(imageView.context))
    }
}

@BindingAdapter("eventHeaderAnim")
fun eventHeaderAnim(lottieView: LottieAnimationView, session: Session?) {
    val anim = when (session?.sessionType) {
        SessionType.SPEAK -> "anim/speak.json"
        SessionType.WORKSHOP -> "anim/workshop.json"
        SessionType.DEBATE -> "anim/debate.json"
        SessionType.CONCLUSION -> "anim/ending.json"
        else -> "anim/speak.json"
    }

    lottieView.setAnimation(anim)
    lottieView.playAnimation()
    lottieView.repeatCount = LottieDrawable.INFINITE
}

@BindingAdapter(
    value = ["sessionDetailStartTime", "sessionDetailEndTime", "timeZoneId"], requireAll = true
)
fun timeString(
    view: TextView,
    sessionDetailStartTime: ZonedDateTime?,
    sessionDetailEndTime: ZonedDateTime?,
    timeZoneId: ZoneId?
) {
    if (sessionDetailStartTime == null || sessionDetailEndTime == null || timeZoneId == null) {
        view.text = ""
    } else {
        view.text = TimeUtils.timeString(
            TimeUtils.zonedTime(sessionDetailStartTime, timeZoneId),
            TimeUtils.zonedTime(sessionDetailEndTime, timeZoneId)
        )
    }
}

@BindingAdapter("sessionStartCountdown")
fun sessionStartCountdown(view: TextView, timeUntilStart: Duration?) {
    if (timeUntilStart == null) {
        view.visibility = GONE
    } else {
        view.visibility = VISIBLE
        val minutes = timeUntilStart.toMinutes()
        view.text = view.context.resources.getQuantityString(
            R.plurals.session_starting_in, minutes.toInt(), minutes.toString()
        )
    }
}