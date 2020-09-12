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

import androidx.annotation.StringRes
import dev.forcetower.conference.R
import dev.forcetower.conference.core.model.persistence.UserEvent

enum class ReservationViewState(
    val state: IntArray,
    @StringRes val text: Int
) {
    RESERVABLE(
        intArrayOf(R.attr.state_reservable),
        R.string.reservation_reservable
    ),
    WAIT_LIST_AVAILABLE(
        intArrayOf(R.attr.state_wait_list_available),
        R.string.reservation_waitlist_available
    ),
    WAIT_LISTED(
        intArrayOf(R.attr.state_wait_listed),
        R.string.reservation_waitlisted
    ),
    RESERVED(
        intArrayOf(R.attr.state_reserved),
        R.string.reservation_reserved
    ),
    RESERVATION_PENDING(
        intArrayOf(R.attr.state_reservation_pending),
        R.string.reservation_pending
    ),
    RESERVATION_DISABLED(
        intArrayOf(R.attr.state_reservation_disabled),
        R.string.reservation_disabled
    );

    companion object {
        fun fromUserEvent(userEvent: UserEvent?, unavailable: Boolean): ReservationViewState {
            return when {
                userEvent?.isReservationPending() == true -> RESERVATION_PENDING
                userEvent?.isReserved() == true -> RESERVED
                userEvent?.isWaitlisted() == true -> WAIT_LISTED
                unavailable -> RESERVATION_DISABLED
                else -> RESERVABLE
            }
        }
    }
}
