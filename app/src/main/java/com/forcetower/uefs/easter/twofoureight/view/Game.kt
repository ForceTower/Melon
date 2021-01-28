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

import timber.log.Timber
import java.util.UUID
import kotlin.math.pow

/**
 * Created by João Paulo on 02/06/2018.
 */
class Game {
    private var endingMaxValue: Int = 0
    var gameGrid: GameGrid? = null
        private set
    var uuid: String = UUID.randomUUID().toString()
    private val mPositionsX = DEFAULT_HEIGHT_X
    private val mPositionsY = DEFAULT_WIDTH_Y
    private val mTileTypes = DEFAULT_TILE_TYPES
    private val mStartingTiles = DEFAULT_STARTING_TILES
    var isCanUndo: Boolean = false
    var lastGameState: State? = null
    private var mBufferGameState: State? = null
    var gameState: State? = State.NORMAL
        private set
    private var mView: GameView? = null
    private var mScoreListener: ScoreListener? = null
    var score: Long = 0
    var lastScore: Long = 0
    private var mBufferScore: Long = 0
    private var mGameStateListener: GameStateListener? = null

    private val isGameWon: Boolean
        get() = gameState == State.WON || gameState == State.ENDLESS_WON

    val isGameOnGoing: Boolean
        get() = gameState != State.WON && gameState != State.LOST && gameState != State.ENDLESS_WON

    val isEndlessMode: Boolean
        get() = gameState == State.ENDLESS || gameState == State.ENDLESS_WON

    private val isMovePossible: Boolean
        get() = gameGrid!!.isCellsAvailable || tileMatchesAvailable()

    enum class State {
        NORMAL, WON, LOST, ENDLESS, ENDLESS_WON
    }

    interface ScoreListener {
        fun onNewScore(score: Long)
    }

    interface GameStateListener {
        fun onGameStateChanged(state: State?)
    }

    fun setGameStateListener(listener: GameStateListener) {
        this.mGameStateListener = listener
    }

    fun setScoreListener(listener: ScoreListener) {
        mScoreListener = listener
    }

    fun setup(view: GameView) {
        mView = view
    }

    private fun updateScore(score: Long) {
        this.score = score
        if (mScoreListener != null)
            mScoreListener!!.onNewScore(this.score)
    }

    fun newGame() {
        uuid = UUID.randomUUID().toString()
        if (gameGrid == null) {
            gameGrid = GameGrid(mPositionsX, mPositionsY)
        } else {
            prepareUndoState()
            saveUndoState()
            gameGrid!!.clearGrid()
        }
        endingMaxValue = 2.0.pow((mTileTypes - 1).toDouble()).toInt()
        mView!!.updateGrid(gameGrid!!)

        updateScore(0)
        updateGameState(State.NORMAL)
        mView!!.setGameState(gameState!!)
        addStartTiles()
        mView!!.setRefreshLastTime(true)
        mView!!.reSyncTime()
        mView!!.invalidate()
    }

    private fun addStartTiles() {
        for (xx in 0 until mStartingTiles) {
            addRandomTile()
        }
    }

    private fun addRandomTile() {
        if (gameGrid!!.isCellsAvailable) {
            val value = if (Math.random() < 0.9) 2 else 4
            val tile = Tile(gameGrid!!.randomAvailableCell()!!, value)
            spawnTile(tile)
        }
    }

    private fun spawnTile(tile: Tile) {
        gameGrid!!.insertTile(tile)
        mView!!.spawnTile(tile)
    }

    private fun prepareTiles() {
        for (array in gameGrid!!.grid) {
            for (tile in array) {
                if (gameGrid!!.isCellOccupied(tile)) {
                    tile.mergedFrom = null
                }
            }
        }
    }

    private fun moveTile(tile: Tile, cell: Position) {
        gameGrid!!.grid[tile.x][tile.y] = null
        gameGrid!!.grid[cell.x][cell.y] = tile
        tile.updatePosition(cell)
    }

    private fun saveUndoState() {
        gameGrid!!.saveTiles()
        isCanUndo = true
        lastScore = mBufferScore
        lastGameState = mBufferGameState
    }

    private fun prepareUndoState() {
        gameGrid!!.prepareSaveTiles()
        mBufferScore = score
        mBufferGameState = gameState
    }

    fun revertUndoState() {
        if (gameState != State.WON) {
            if (isCanUndo) {
                isCanUndo = false
                mView!!.cancelAnimations()
                gameGrid!!.revertTiles()
                updateScore(lastScore)
                updateGameState(lastGameState)
                mView!!.setGameState(gameState!!)
                mView!!.setRefreshLastTime(true)
                mView!!.invalidate()
            }
        } else {
            Timber.d("Can't revert a won game")
        }
    }

    fun updateUI() {
        updateScore(score)
        mView!!.setGameState(gameState!!)
        mView!!.setRefreshLastTime(true)
        mView!!.invalidate()
    }

    fun move(direction: Int) {
        mView!!.cancelAnimations()
        if (!isGameOnGoing) {
            Timber.d("Game is not happening")
            return
        }

        Timber.d("Game will process your move")
        prepareUndoState()
        val vector = Position.getVector(direction)
        val traversalsX = buildTraversalsX(vector)
        val traversalsY = buildTraversalsY(vector)
        var moved = false

        prepareTiles()

        for (xx in traversalsX) {
            for (yy in traversalsY) {
                val cell = Position(xx, yy)
                val tile = gameGrid!!.getTile(cell)

                if (tile != null) {
                    val positions = findFarthestPosition(cell, vector)
                    val next = gameGrid!!.getTile(positions[1])

                    if (next != null && next.value == tile.value && next.mergedFrom == null) {
                        val merged = Tile(positions[1], tile.value * 2)
                        val temp = arrayOf(tile, next)
                        merged.mergedFrom = temp

                        gameGrid!!.insertTile(merged)
                        gameGrid!!.removeTile(tile)

                        // Converge the two tiles' positions
                        tile.updatePosition(positions[1])

                        val extras = intArrayOf(xx, yy)
                        // Direction: 0 = MOVING MERGED
                        mView!!.moveTile(merged.x, merged.y, extras)
                        mView!!.mergeTile(merged.x, merged.y)

                        updateScore(score + merged.value)

                        // The mighty 2048 tile
                        if (merged.value >= winValue() && !isGameWon) {
                            when (gameState) {
                                State.ENDLESS -> updateGameState(State.ENDLESS_WON)
                                State.NORMAL -> updateGameState(State.WON)
                                else -> throw RuntimeException("Can't move into win state")
                            }
                            mView!!.setGameState(gameState!!)
                            endGame()
                        }
                    } else {
                        moveTile(tile, positions[0])
                        val extras = intArrayOf(xx, yy, 0)
                        // Direction: 1 = MOVING NO MERGE
                        mView!!.moveTile(positions[0].x, positions[0].y, extras)
                    }

                    if (!Position.equal(cell, tile)) {
                        moved = true
                    }
                }
            }
        }
        mView!!.updateGrid(gameGrid!!)
        if (moved) {
            saveUndoState()
            addRandomTile()
            checkLose()
        }
        mView!!.reSyncTime()
        mView!!.invalidate()
    }

    private fun findFarthestPosition(cell: Position, vector: Position): Array<Position> {
        var previous: Position
        var nextCell = Position(cell.x, cell.y)
        do {
            previous = nextCell
            nextCell = Position(
                previous.x + vector.x,
                previous.y + vector.y
            )
        } while (gameGrid!!.isCellWithinBounds(nextCell) && gameGrid!!.isCellAvailable(nextCell))
        return arrayOf(previous, nextCell)
    }

    fun updateGameState(state: State?) {
        gameState = state
        if (mGameStateListener != null)
            mGameStateListener!!.onGameStateChanged(gameState)
    }

    private fun checkLose() {
        if (!isMovePossible && !isGameWon) {
            updateGameState(State.LOST)
            mView!!.setGameState(gameState!!)
            endGame()
        }
    }

    private fun endGame() {
        mView!!.endGame()
        updateScore(score)
    }

    private fun buildTraversalsX(vector: Position): List<Int> {
        val traversals = ArrayList<Int>()
        for (xx in 0 until mPositionsX) {
            traversals.add(xx)
        }
        if (vector.x == 1) {
            traversals.reverse()
        }
        return traversals
    }

    private fun buildTraversalsY(vector: Position): List<Int> {
        val traversals = ArrayList<Int>()
        for (xx in 0 until mPositionsY) {
            traversals.add(xx)
        }
        if (vector.y == 1) {
            traversals.reverse()
        }
        return traversals
    }

    private fun tileMatchesAvailable(): Boolean {
        var tile: Tile?
        for (xx in 0 until mPositionsX) {
            for (yy in 0 until mPositionsY) {
                tile = gameGrid!!.getTile(Position(xx, yy))
                if (tile != null) {
                    for (direction in 0..3) {
                        val vector = Position.getVector(direction)
                        val cell = Position(xx + vector.x, yy + vector.y)
                        val other = gameGrid!!.getTile(cell)
                        if (other != null && other.value == tile.value) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    private fun winValue(): Int {
        return if (isEndlessMode) {
            endingMaxValue
        } else {
            startingMaxValue
        }
    }

    fun setEndlessMode() {
        updateGameState(State.ENDLESS)
        mView!!.setGameState(gameState!!)
        mView!!.invalidate()
        mView!!.setRefreshLastTime(true)
    }

    companion object {
        private const val startingMaxValue = 2048
        internal const val DEFAULT_HEIGHT_X = 4
        internal const val DEFAULT_WIDTH_Y = 4
        internal const val DEFAULT_TILE_TYPES = 24
        private const val DEFAULT_STARTING_TILES = 2

        const val DIRECTION_UP = 0
        const val DIRECTION_RIGHT = 1
        const val DIRECTION_DOWN = 2
        const val DIRECTION_LEFT = 3
    }
}
