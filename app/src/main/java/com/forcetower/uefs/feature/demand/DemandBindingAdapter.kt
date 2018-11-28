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

package com.forcetower.uefs.feature.demand

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.forcetower.sagres.database.model.SDemandOffer
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R
import com.google.android.material.card.MaterialCardView

@BindingAdapter(value = ["disciplineIcon"])
fun disciplineIcon(iv: ImageView, offer: SDemandOffer?) {
    offer ?: return

    val drawable = when {
        !offer.selectable -> R.drawable.ic_bug_report_black_24dp
        offer.completed -> R.drawable.ic_done_outline_black_24dp
        offer.unavailable -> R.drawable.ic_lock_black_24dp
        offer.selected -> R.drawable.ic_offline_flash_black_24dp
        offer.current -> R.drawable.ic_change_history_black_24dp
        offer.available -> R.drawable.ic_star_black_24dp
        else -> R.drawable.ic_bug_report_black_24dp
    }

    GlideApp.with(iv.context).load(drawable).fitCenter().into(iv)
}

@BindingAdapter(value = ["animatedStrokeColor"])
fun strokeColor(cardView: MaterialCardView, offer: SDemandOffer?) {
    offer ?: return

    val ctx = cardView.context
    val old = cardView.strokeColor

    val next = when {
        !offer.selectable -> R.color.demand_case_bugged
        offer.completed -> R.color.demand_case_completed
        offer.unavailable -> R.color.demand_case_unavailable
        offer.selected -> R.color.demand_case_selected
        offer.current -> R.color.demand_case_current
        offer.available -> R.color.demand_case_available
        else -> R.color.demand_case_bugged
    }

    val tgt = ctx.getColor(next)
    if (tgt == old) return

    cardView.strokeColor = tgt

//    val animation = ValueAnimator()
//    animation.setIntValues(old, tgt)
//    animation.setEvaluator(ArgbEvaluator())
//    animation.duration = 500
//    animation.repeatCount = 1
//
//    animation.addUpdateListener {
//        cardView.strokeColor = it.animatedValue as Int
//    }
//
//    animation.start()
}
