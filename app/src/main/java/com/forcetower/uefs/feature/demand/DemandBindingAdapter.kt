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

package com.forcetower.uefs.feature.demand

import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.forcetower.sagres.database.model.SagresDemandOffer
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R
import com.google.android.material.card.MaterialCardView

@BindingAdapter(value = ["disciplineIcon"])
fun disciplineIcon(iv: ImageView, offer: SagresDemandOffer?) {
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
fun strokeColor(cardView: MaterialCardView, offer: SagresDemandOffer?) {
    offer ?: return

    val ctx = cardView.context
    val old = cardView.strokeColorStateList?.defaultColor

    val next = when {
        !offer.selectable -> R.color.demand_case_bugged
        offer.completed -> R.color.demand_case_completed
        offer.unavailable -> R.color.demand_case_unavailable
        offer.selected -> R.color.demand_case_selected
        offer.current -> R.color.demand_case_current
        offer.available -> R.color.demand_case_available
        else -> R.color.demand_case_bugged
    }

    val tgt = ContextCompat.getColor(ctx, next)
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
