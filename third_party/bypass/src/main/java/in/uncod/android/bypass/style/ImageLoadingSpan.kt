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

package `in`.uncod.android.bypass.style

import android.text.TextPaint
import android.text.style.CharacterStyle

/**
 * A simple text span used to mark text that will be replaced by an image once it has been
 * downloaded. See [in.uncod.android.bypass.Bypass.LoadImageCallback]
 */
class ImageLoadingSpan : CharacterStyle() {
    override fun updateDrawState(textPaint: TextPaint) {
        // no-op
    }
}
