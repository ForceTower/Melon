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

package com.forcetower.uefs.easter.twofoureight.view

open class Position(x: Int, y: Int) {
    var x: Int = 0
        internal set
    var y: Int = 0
        internal set

    init {
        this.x = x
        this.y = y
    }

    companion object {

        fun equal(first: Position, second: Position): Boolean {
            return first.x == second.x && first.y == second.y
        }

        fun getVector(direction: Int): Position {
            val map = arrayOf(
                Position(0, -1), // up
                Position(1, 0), // right
                Position(0, 1), // down
                Position(-1, 0) // left
            )
            return map[direction]
        }
    }
}
