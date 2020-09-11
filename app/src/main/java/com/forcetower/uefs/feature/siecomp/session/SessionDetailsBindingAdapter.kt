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
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

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
    value = ["sessionDetailStartTime", "sessionDetailEndTime", "timeZoneId"],
    requireAll = true
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
            R.plurals.session_starting_in,
            minutes.toInt(),
            minutes.toString()
        )
    }
}
