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

package com.forcetower.uefs.feature.syncregistry

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.NetworkType
import com.forcetower.uefs.feature.shared.formatFullDate

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