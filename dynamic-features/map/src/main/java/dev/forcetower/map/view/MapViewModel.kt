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

package dev.forcetower.map.view

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

class MapViewModel : ViewModel(), MapActions {
    override val campusLocationBounds = LatLngBounds(UEFS_MAP_VIEWPORT_BOUND_SW, UEFS_MAP_VIEWPORT_BOUND_NE)

    companion object {
        private val UEFS_MAP_VIEWPORT_BOUND_NE = LatLng(-12.195484, -38.964935)
        private val UEFS_MAP_VIEWPORT_BOUND_SW = LatLng(-12.205645, -38.976297)
    }
}
