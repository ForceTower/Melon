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

package com.forcetower.uefs.feature.syncregistry

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.NetworkType
import com.forcetower.uefs.feature.shared.extensions.formatFullDate

@BindingAdapter("syncTime")
fun bindTime(tv: TextView, value: Long?) {
    if (value == null) tv.text = "..."
    else {
        val date = value.formatFullDate()
        tv.text = date
    }
}

@BindingAdapter(value = ["network", "networkType"], requireAll = true)
fun bindNetwork(tv: TextView, network: String, networkType: Int) {
    val drawable = if (networkType == NetworkType.WIFI.ordinal)
        R.drawable.ic_network_wifi_black_24dp
    else
        R.drawable.ic_network_cell_black_24dp

    tv.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0)
    tv.text = network
}

@BindingAdapter(value = ["syncComplete", "syncStatus"])
fun bindStatus(tv: TextView, syncComplete: Boolean, syncStatus: Boolean) {
    val message = if (!syncComplete)
        tv.context.getString(R.string.sync_incomplete)
    else if (syncStatus)
        tv.context.getString(R.string.sync_completed)
    else
        tv.context.getString(R.string.sync_failed)

    tv.text = message
}
