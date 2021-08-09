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

package dev.forcetower.conference.core.model.persistence

data class UserEvent(
    val sessionId: String,
    val starred: Boolean,
    val reviewed: Boolean,
    val requesting: Boolean,
    val reservationStatus: ReservationStatus = ReservationStatus.NONE
) {
    fun isPinned(): Boolean {
        return starred || isReserved() || isWaitlisted()
    }

    fun isReserved(): Boolean {
        return reservationStatus == ReservationStatus.RESERVED
    }

    fun isWaitlisted(): Boolean {
        return reservationStatus == ReservationStatus.WAITLISTED
    }

    fun isReservationPending(): Boolean {
        return requesting
    }

    enum class ReservationStatus {
        RESERVED,
        WAITLISTED,
        NONE;

        companion object {
            fun getIfPresent(string: String): ReservationStatus? {
                return try {
                    valueOf(string)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
    }
}
