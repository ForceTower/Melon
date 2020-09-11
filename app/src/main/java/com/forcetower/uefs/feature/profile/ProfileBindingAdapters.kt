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

package com.forcetower.uefs.feature.profile

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.preference.PreferenceManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Semester
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

@BindingAdapter("profileImage")
fun profileImage(iv: ImageView, url: String?) {
    if (url == null) return

    GlideApp.with(iv.context)
        .load(url)
        .fallback(R.mipmap.ic_unes_large_image_512)
        .placeholder(R.mipmap.ic_unes_large_image_512)
        .circleCrop()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(iv)
}

@BindingAdapter(requireAll = true, value = ["firebaseUser", "firebaseStorage"])
fun firebaseUser(iv: ImageView, user: FirebaseUser?, storage: FirebaseStorage) {
    if (user != null) {
        val reference = storage.getReference("users/${user.uid}/avatar.jpg")
        try {
            GlideApp.with(iv.context)
                .load(reference)
                .fallback(R.mipmap.ic_unes_large_image_512)
                .placeholder(R.mipmap.ic_unes_large_image_512)
                .signature(ObjectKey(System.currentTimeMillis() ushr 21))
                .circleCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(iv)
        } catch (ignored: Throwable) {
        }
    } else {
        GlideApp.with(iv.context)
            .load(R.mipmap.ic_unes_large_image_512)
            .fallback(R.mipmap.ic_unes_large_image_512)
            .placeholder(R.mipmap.ic_unes_large_image_512)
            .circleCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(iv)
    }
}

@BindingAdapter(value = ["profileScoreOptional", "profileScoreCalculated", "semestersList", "profileCourse"], requireAll = true)
fun profileScoreOptional(
    tv: TextView,
    score: Double?,
    calculated: Double?,
    semesters: List<Semester>?,
    course: String?
) {
    val context = tv.context
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val actual = score ?: -1.0
    val calc = calculated ?: -1.0

    // power up da nota
    var currentIncrease = preferences.getFloat("score_increase_value", 0f)
    val currentExpire = preferences.getLong("score_increase_expires", -1)

    val now = Calendar.getInstance().timeInMillis
    if (currentExpire < now) currentIncrease = 0.0f

    if (preferences.getBoolean("stg_acc_score", true)) {
        // caso queira o calculado
        if (preferences.getBoolean("stg_choice_score", false))
            when {
                calc != -1.0 -> tv.text = context.getString(R.string.label_your_calculated_score, min((calc + currentIncrease), 10.0))
                actual != -1.0 -> tv.text = context.getString(R.string.label_your_score, min((actual + currentIncrease), 10.0))
            }
        // por padrão exibe o real que vem do SAGRES
        else
            when {
                actual != -1.0 -> tv.text = context.getString(R.string.label_your_score, min((actual + currentIncrease), 10.0))
                calc != -1.0 -> tv.text = context.getString(R.string.label_your_calculated_score, min((calc + currentIncrease), 10.0))
            }

        // verificando se existe realmente um score
        if (calc / actual == 1.0 && calc == -1.0) tv.text = context.getString(R.string.label_score_undefined)
    } else if (preferences.getBoolean("stg_acc_semester", true)) {
        val filtered = semesters?.filter { !it.name.endsWith("F") }
        val number = filtered?.size ?: 1
        tv.text = context.getString(R.string.your_semester_is, number)
    } else if (course != null) {
        tv.text = course
    } else {
        tv.text = ""
    }
}

@BindingAdapter(value = ["zonedStatement"])
fun getZonedTimeStampedDate(view: TextView, zonedDate: ZonedDateTime?) {
    if (zonedDate == null) {
        view.visibility = View.INVISIBLE
        return
    }
    val time = zonedDate.toLocalDateTime().toInstant(ZoneOffset.ofHours(0)).toEpochMilli()
    val context = view.context
    val now = System.currentTimeMillis()
    val diff = now - time

    val oneDay = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)
    val oneHor = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)
    val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
    val value = when {
        days > 1L -> {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val str = format.format(Date(time))
            context.getString(R.string.profile_statement_received_date_format, str)
        }
        days == 1L -> {
            val hours = TimeUnit.HOURS.convert(diff - oneDay, TimeUnit.MILLISECONDS)
            val str = days.toString() + "d " + hours.toString() + "h"
            context.getString(R.string.profile_statement_received_date_ago_format, str)
        }
        else -> {
            val hours = TimeUnit.HOURS.convert(diff, TimeUnit.MILLISECONDS)
            val minutes = TimeUnit.MINUTES.convert(diff - (hours * oneHor), TimeUnit.MILLISECONDS)
            val str = if (hours > 0) {
                hours.toString() + "h " + minutes + "min"
            } else {
                minutes.toString() + "min"
            }
            context.getString(R.string.message_received_date_ago_format, str)
        }
    }
    view.text = value
    view.visibility = View.VISIBLE
}
