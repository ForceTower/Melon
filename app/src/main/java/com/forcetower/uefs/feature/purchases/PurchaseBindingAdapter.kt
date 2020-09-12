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

package com.forcetower.uefs.feature.purchases

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.uefs.R

@BindingAdapter(value = ["skuPrice"])
fun price(tv: TextView, price: String?) {
    val value = price ?: "???,??"
    tv.text = tv.context.getString(R.string.sku_price_format, "R$", value)
}

@BindingAdapter(value = ["skuTitle"])
fun title(tv: TextView, title: String?) {
    val value = title ?: "Nem sei"
    if (value.contains("(")) {
        val index = value.lastIndexOf("(")
        val corrected = value.substring(0, index).trim()
        tv.text = corrected
    } else {
        tv.text = value
    }
}
