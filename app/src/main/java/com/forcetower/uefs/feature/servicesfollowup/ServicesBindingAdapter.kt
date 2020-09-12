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

package com.forcetower.uefs.feature.servicesfollowup

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.forcetower.uefs.R
import java.util.Locale

@BindingAdapter("requestedServiceImage")
fun requestedServiceImage(iv: ImageView, situation: String?) {
    situation ?: return
    val ctx = iv.context
    val icon = when (situation.toLowerCase(Locale.getDefault())) {
        "atendido" -> R.drawable.ic_check_black_24dp
        "indeferido" -> R.drawable.ic_block_black_24dp
        else -> R.drawable.ic_change_history_black_24dp
    }
    val drawable = ctx.getDrawable(icon)
    iv.setImageDrawable(drawable)
}
