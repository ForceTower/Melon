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

package dev.forcetower.conference.core.ui.reservation

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import dev.forcetower.conference.R

class ReservationTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    var status = ReservationViewState.RESERVABLE
        set(value) {
            if (value == field) return
            field = value
            setText(value.text)
            refreshDrawableState()
        }

    init {
        setText(ReservationViewState.RESERVABLE.text)
        val drawable = ContextCompat.getDrawable(context, R.drawable.asld_reservation)
        setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        @Suppress("SENSELESS_COMPARISON") // Status is null during super init
        if (status == null) return super.onCreateDrawableState(extraSpace)
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(drawableState, status.state)
        return drawableState
    }
}
