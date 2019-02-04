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

package com.forcetower.uefs.feature.profile

import android.preference.PreferenceManager
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import java.util.*
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

@BindingAdapter(value = ["profileScoreOptional", "profileScoreCalculated"], requireAll = true)
fun profileScoreOptional(tv: TextView, score: Double?, calculated: Double?) {
    val context = tv.context
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val actual = score ?: -1.0
    val calc = calculated ?: -1.0

    //power up da nota
    var currentIncrease = preferences.getFloat("score_increase_value", 0f)
    val currentExpire = preferences.getLong("score_increase_expires", -1)

    val now = Calendar.getInstance().timeInMillis
    if (currentExpire < now) currentIncrease = 0.0f

    if (preferences.getBoolean("stg_acc_score", true)) {
        //caso queira o calculado
        if (preferences.getBoolean("stg_choice_score", false))
            when {
                calc != -1.0 -> tv.text = context.getString(R.string.label_your_calculated_score, min((calc + currentIncrease), 10.0))
                actual != -1.0 -> tv.text = context.getString(R.string.label_your_score, min((actual + currentIncrease), 10.0))
            }
        //por padrão exibe o real que vem do SAGRES
        else
            when {
                actual != -1.0 -> tv.text = context.getString(R.string.label_your_score, min((actual + currentIncrease), 10.0))
                calc != -1.0 -> tv.text = context.getString(R.string.label_your_calculated_score, min((calc + currentIncrease), 10.0))
            }

        //verificando se existe realmente um score
        if(calc/actual == 1.0 && calc == -1.0) tv.text = context.getString(R.string.label_score_undefined)
    } else {
        tv.visibility = View.INVISIBLE
    }
}