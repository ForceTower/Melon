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

package dev.forcetower.conference.core.model.reservation

data class ReservationRequestResult(
    val requestResult: ReservationRequestStatus? = null,
    val requestId: String,
    val timestamp: Long
) {

    enum class ReservationRequestStatus {
        /** The reservation was granted */
        RESERVE_SUCCEEDED,

        /** The reservation was granted but the user was placed on a waitlist. */
        RESERVE_WAITLISTED,

        /** The reservation request was denied because it was too close to the start of the
         * event. */
        RESERVE_DENIED_CUTOFF,

        /** The reservation was denied because it overlapped with another reservation or
         * waitlist. */
        RESERVE_DENIED_CLASH,

        /** The reservation was denied for unknown reasons. */
        RESERVE_DENIED_UNKNOWN,

        /** The reservation was successfully canceled. */
        CANCEL_SUCCEEDED,

        /** The cancellation request was denied because it was too close to the start of
         * the event. */
        CANCEL_DENIED_CUTOFF,

        /** The cancellation request was denied for unknown reasons. */
        CANCEL_DENIED_UNKNOWN,

        /** The reservation was granted by a Swap request. */
        SWAP_SUCCEEDED,

        /** The reservation was granted but the user was placed on a waitlist by a Swap request. */
        SWAP_WAITLISTED,

        /** The reservation request was denied because it was too close to the start of the
         * event by a Swap request. */
        SWAP_DENIED_CUTOFF,

        /** The reservation was denied because it overlapped with another reservation or
         * waitlist by a Swap request. */
        SWAP_DENIED_CLASH,

        /** The reservation was denied for unknown reasons by a Swap request. */
        SWAP_DENIED_UNKNOWN;

        companion object {
            fun getIfPresent(string: String): ReservationRequestStatus? {
                return try {
                    valueOf(string)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
    }
}
