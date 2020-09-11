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

package com.forcetower.uefs.easter.twofoureight.tools

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import com.forcetower.uefs.easter.twofoureight.view.Game
import kotlin.math.abs

class InputListener : View.OnTouchListener {

    private var x: Float = 0.toFloat()
    private var y: Float = 0.toFloat()
    private var lastdx: Float = 0.toFloat()
    private var lastdy: Float = 0.toFloat()
    private var previousX: Float = 0.toFloat()
    private var previousY: Float = 0.toFloat()
    private var startingX: Float = 0.toFloat()
    private var startingY: Float = 0.toFloat()
    private var previousDirection = 1
    private var veryLastDirection = 1
    private var hasMoved = false

    private var mView: View? = null
    private var mGame: Game? = null

    fun setView(view: View) {
        mView = view
        mView!!.setOnTouchListener(this)
    }

    fun setGame(game: Game) {
        mGame = game
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                x = event.x
                y = event.y
                startingX = x
                startingY = y
                previousX = x
                previousY = y
                lastdx = 0f
                lastdy = 0f
                hasMoved = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                x = event.x
                y = event.y
                if (mGame!!.isGameOnGoing) {
                    val dx = x - previousX
                    if (abs(lastdx + dx) < abs(lastdx) + abs(dx) && abs(dx) > RESET_STARTING &&
                        abs(x - startingX) > SWIPE_MIN_DISTANCE
                    ) {
                        startingX = x
                        startingY = y
                        lastdx = dx
                        previousDirection = veryLastDirection
                    }
                    if (lastdx == 0f) {
                        lastdx = dx
                    }
                    val dy = y - previousY
                    if (abs(lastdy + dy) < abs(lastdy) + abs(dy) && abs(dy) > RESET_STARTING &&
                        abs(y - startingY) > SWIPE_MIN_DISTANCE
                    ) {
                        startingX = x
                        startingY = y
                        lastdy = dy
                        previousDirection = veryLastDirection
                    }
                    if (lastdy == 0f) {
                        lastdy = dy
                    }
                    if (pathMoved() > SWIPE_MIN_DISTANCE * SWIPE_MIN_DISTANCE && !hasMoved) {
                        var moved = false
                        // Vertical
                        if ((dy >= SWIPE_THRESHOLD_VELOCITY && abs(dy) >= abs(dx) || y - startingY >= MOVE_THRESHOLD) && previousDirection % 2 != 0) {
                            moved = true
                            previousDirection *= 2
                            veryLastDirection = 2
                            mGame!!.move(2)
                        } else if ((dy <= -SWIPE_THRESHOLD_VELOCITY && abs(dy) >= abs(dx) || y - startingY <= -MOVE_THRESHOLD) && previousDirection % 3 != 0) {
                            moved = true
                            previousDirection *= 3
                            veryLastDirection = 3
                            mGame!!.move(0)
                        }
                        // Horizontal
                        if ((dx >= SWIPE_THRESHOLD_VELOCITY && abs(dx) >= abs(dy) || x - startingX >= MOVE_THRESHOLD) && previousDirection % 5 != 0) {
                            moved = true
                            previousDirection *= 5
                            veryLastDirection = 5
                            mGame!!.move(1)
                        } else if ((dx <= -SWIPE_THRESHOLD_VELOCITY && abs(dx) >= abs(dy) || x - startingX <= -MOVE_THRESHOLD) && previousDirection % 7 != 0) {
                            moved = true
                            previousDirection *= 7
                            veryLastDirection = 7
                            mGame!!.move(3)
                        }
                        if (moved) {
                            hasMoved = true
                            startingX = x
                            startingY = y
                        }
                    }
                }
                previousX = x
                previousY = y
                return true
            }
            MotionEvent.ACTION_UP -> {
                x = event.x
                y = event.y
                previousDirection = 1
                veryLastDirection = 1
            }
        }
        return true
    }

    private fun pathMoved(): Float {
        return (x - startingX) * (x - startingX) + (y - startingY) * (y - startingY)
    }

    companion object {

        private const val SWIPE_MIN_DISTANCE = 0
        private const val SWIPE_THRESHOLD_VELOCITY = 25
        private const val MOVE_THRESHOLD = 250
        private const val RESET_STARTING = 10
    }
}
