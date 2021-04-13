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

package dev.forcetower.map.core.map

import com.google.android.gms.maps.model.UrlTileProvider
import java.net.URL

class MapTileProvider(tileSize: Int = BASE_TILE_SIZE) : UrlTileProvider(tileSize, tileSize) {
    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL {
        return URL(BASE_URL.format(zoom, x, y))
    }

    companion object {
        private const val BASE_TILE_SIZE = 256
        private const val BASE_URL = "https://storage.googleapis.com/unes_map_titles/tiles/%d/%d/%d.png"
    }
}
