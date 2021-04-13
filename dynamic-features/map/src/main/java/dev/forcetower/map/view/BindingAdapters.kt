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

import androidx.databinding.BindingAdapter
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLngBounds

/**
 * Sets the map viewport to a specific rectangle specified by two Latitude/Longitude points.
 */
@BindingAdapter("mapViewport")
fun mapViewport(mapView: MapView, bounds: LatLngBounds?) {
    if (bounds != null) {
        mapView.getMapAsync {
            it.setLatLngBoundsForCameraTarget(bounds)
        }
    }
}

/**
 * Sets the center of the map's camera. Call this every time the user selects a marker.
 */
// @BindingAdapter("mapCenter")
// fun mapCenter(mapView: MapView, event: Event<CameraUpdate>?) {
//    val update = event?.getContentIfNotHandled() ?: return
//    mapView.getMapAsync {
//        it.animateCamera(update)
//    }
// }

@BindingAdapter("mapMinZoom", "mapMaxZoom", requireAll = true)
fun mapZoomLevels(mapView: MapView, minZoom: Float, maxZoom: Float) {
    mapView.getMapAsync {
        it.setMinZoomPreference(minZoom)
        it.setMaxZoomPreference(maxZoom)
    }
}

@BindingAdapter("isIndoorEnabled")
fun isIndoorEnabled(mapView: MapView, isIndoorEnabled: Boolean?) {
    if (isIndoorEnabled != null) {
        mapView.getMapAsync {
            it.isIndoorEnabled = isIndoorEnabled
        }
    }
}

@BindingAdapter("isMapToolbarEnabled")
fun isMapToolbarEnabled(mapView: MapView, isMapToolbarEnabled: Boolean?) {
    if (isMapToolbarEnabled != null) {
        mapView.getMapAsync {
            it.uiSettings.isMapToolbarEnabled = isMapToolbarEnabled
        }
    }
}
