/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.core.model.service

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Event(
    var id: String = "",
    var name: String = "Sem nome",
    var description: String = "Nada especificado",
    var imageUrl: String? = null,
    var creatorName: String? = null,
    var creatorId: String? = null,
    var offeredBy: String = "Ninguem",
    var startDate: Long = 0,
    var endDate: Long = 0,
    var location: String = "Não especificado",
    var price: Double? = null,
    var certificateHours: Int? = null,
    var courseId: Int? = null,
    var featured: Boolean = false,
    @ServerTimestamp
    var createdAt: Timestamp? = null
) {

    override fun toString(): String {
        return name
    }

    companion object {
        const val COLLECTION = "events"
    }
}