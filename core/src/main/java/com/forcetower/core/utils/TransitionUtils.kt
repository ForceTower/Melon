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

package com.forcetower.core.utils

import android.view.View
import android.view.ViewGroup

object TransitionUtils {
    fun setAncestralClipping(
        view: View,
        clipChildren: Boolean
    ): List<Boolean> {
        return setAncestralClipping(view, clipChildren, mutableListOf())
    }

    private fun setAncestralClipping(
        view: View,
        clipChildren: Boolean,
        was: MutableList<Boolean>
    ): List<Boolean> {
        if (view is ViewGroup) {
            was.add(view.clipChildren)
            view.clipChildren = clipChildren
        }
        val parent = view.parent
        if (parent != null && parent is ViewGroup) {
            setAncestralClipping(parent, clipChildren, was)
        }
        return was
    }

    fun restoreAncestralClipping(
        view: View,
        was: MutableList<Boolean>
    ) {
        if (view is ViewGroup) {
            view.clipChildren = was.removeAt(0)
        }
        val parent = view.parent
        if (parent != null && parent is ViewGroup) {
            restoreAncestralClipping(parent, was)
        }
    }
}
