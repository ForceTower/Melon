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

package com.forcetower.uefs.easter.twofoureight

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.GameFragment2048Binding
import com.forcetower.uefs.easter.darktheme.DarkThemeRepository
import com.forcetower.uefs.easter.twofoureight.tools.InputListener
import com.forcetower.uefs.easter.twofoureight.tools.KeyListener
import com.forcetower.uefs.easter.twofoureight.tools.ScoreKeeper
import com.forcetower.uefs.easter.twofoureight.view.Game
import com.forcetower.uefs.easter.twofoureight.view.Tile
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.UGameActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs

/**
 * Created by João Paulo on 02/06/2018.
 */
@AndroidEntryPoint
class Game2048Fragment : UFragment(), KeyListener, Game.GameStateListener, View.OnTouchListener {

    @Inject lateinit var preferences: SharedPreferences
    @Inject lateinit var darkRepository: DarkThemeRepository

    private var downX: Float = 0.toFloat()
    private var downY: Float = 0.toFloat()
    private var upX: Float = 0.toFloat()
    private var upY: Float = 0.toFloat()

    private lateinit var mGame: Game
    private lateinit var binding: GameFragment2048Binding
    private var activity: UGameActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as? UGameActivity
        activity ?: Timber.e("Adventure Fragment must be attached to a UGameActivity for it to work")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.game_fragment_2048, container, false)
        binding.gamePad.setOnTouchListener(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mScoreKeeper = ScoreKeeper(requireActivity())

        mScoreKeeper.setViews(binding.tvScore, binding.tvHighscore)
        mScoreKeeper.setScoreListener(
            object : Game.ScoreListener {
                override fun onNewScore(score: Long) {
                    if (score >= 50000) {
                        unlockDarkTheme()
                    }
                }
            }
        )

        mGame = Game()
        mGame.setup(binding.gameview)
        mGame.setScoreListener(mScoreKeeper)
        mGame.setGameStateListener(this)
        mGame.newGame()
        val input = InputListener()
        input.setView(binding.gameview)
        input.setGame(mGame)

        binding.tvTitle.setOnClickListener {
            if (!mGame.isEndlessMode) {
                mGame.setEndlessMode()
                binding.tvTitle.text = HtmlCompat.fromHtml("&infin;", FROM_HTML_MODE_LEGACY)
            }
        }

        binding.ibReset.setOnClickListener {
            mGame.newGame()
            binding.tvTitle.text = HtmlCompat.fromHtml("2048", FROM_HTML_MODE_LEGACY)
        }

        binding.ibUndo.setOnClickListener {
            mGame.revertUndoState()
            if (mGame.gameState === Game.State.ENDLESS || mGame.gameState === Game.State.ENDLESS_WON) {
                binding.tvTitle.text = HtmlCompat.fromHtml("&infin;", FROM_HTML_MODE_LEGACY)
            } else {
                binding.tvTitle.text = HtmlCompat.fromHtml("2048", FROM_HTML_MODE_LEGACY)
            }
        }

        binding.ibReset.setOnLongClickListener {
            Toast.makeText(activity, getString(R.string.start_new_game), Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun unlockDarkTheme() {
        val enabled = preferences.getBoolean("ach_night_mode_enabled", false)
        if (!enabled) {
            preferences.edit().putBoolean("ach_night_mode_enabled", true).apply()
            darkRepository.getPreconditions()
        }
    }

    override fun onPause() {
        save()
        super.onPause()
    }

    override fun onResume() {
        load()
        if (mGame.gameState === Game.State.ENDLESS || mGame.gameState === Game.State.ENDLESS_WON) {
            binding.tvTitle.text = HtmlCompat.fromHtml("&infin;", FROM_HTML_MODE_LEGACY)
        }
        super.onResume()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                mGame.move(Game.DIRECTION_DOWN)
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                mGame.move(Game.DIRECTION_UP)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                mGame.move(Game.DIRECTION_LEFT)
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                mGame.move(Game.DIRECTION_RIGHT)
                return true
            }
            else -> return false
        }
    }

    override fun onGameStateChanged(state: Game.State?) {
        Timber.d("Game state changed to: %s", state!!)
        if (state == Game.State.WON || state == Game.State.ENDLESS_WON) {
            binding.tvEndgameOverlay.visibility = VISIBLE
            binding.tvEndgameOverlay.setText(R.string.you_win)

            activity?.unlockAchievement(R.string.achievement_voc__bom)
            activity?.incrementAchievementProgress(R.string.achievement_o_campeo_de_2048_no_unes, 1)
        } else if (state == Game.State.LOST) {
            binding.tvEndgameOverlay.visibility = VISIBLE
            binding.tvEndgameOverlay.setText(R.string.game_over)
            activity?.unlockAchievement(R.string.achievement_eu_tentei)
            activity?.incrementAchievementProgress(R.string.achievement_a_prtica_leva__perfeio, 1)
        } else {
            binding.tvEndgameOverlay.visibility = GONE
        }
    }

    private fun save() {
        val settings = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = settings.edit()
        val field = mGame.gameGrid!!.grid
        val undoField = mGame.gameGrid!!.undoGrid
        val uuid = mGame.uuid
        for (xx in field.indices) {
            for (yy in field[0].indices) {
                if (field[xx][yy] != null) {
                    editor.putInt("$xx $yy", field[xx][yy].value)
                } else {
                    editor.putInt("$xx $yy", 0)
                }

                if (undoField[xx][yy] != null) {
                    editor.putInt("$UNDO_GRID$xx $yy", undoField[xx][yy].value)
                } else {
                    editor.putInt("$UNDO_GRID$xx $yy", 0)
                }
            }
        }
        editor.putLong(SCORE, mGame.score)
        editor.putLong(UNDO_SCORE, mGame.lastScore)
        editor.putBoolean(CAN_UNDO, mGame.isCanUndo)
        editor.putString(GAME_STATE, mGame.gameState!!.name)
        editor.putString(UNDO_GAME_STATE, mGame.lastGameState!!.name)
        editor.putString(GAME_UUID, uuid)
        editor.apply()
    }

    private fun load() {
        // Stopping all animations
        binding.gameview.cancelAnimations()
        val settings = PreferenceManager.getDefaultSharedPreferences(activity)
        for (xx in mGame.gameGrid!!.grid.indices) {
            for (yy in mGame.gameGrid!!.grid[0].indices) {
                val value = settings.getInt("$xx $yy", -1)
                if (value > 0) {
                    mGame.gameGrid!!.grid[xx][yy] = Tile(xx, yy, value)
                } else if (value == 0) {
                    mGame.gameGrid!!.grid[xx][yy] = null
                }

                val undoValue = settings.getInt("$UNDO_GRID$xx $yy", -1)
                if (undoValue > 0) {
                    mGame.gameGrid!!.undoGrid[xx][yy] = Tile(xx, yy, undoValue)
                } else if (value == 0) {
                    mGame.gameGrid!!.undoGrid[xx][yy] = null
                }
            }
        }

        mGame.score = settings.getLong(SCORE, 0)
        mGame.lastScore = settings.getLong(UNDO_SCORE, 0)
        mGame.isCanUndo = settings.getBoolean(CAN_UNDO, mGame.isCanUndo)
        mGame.uuid = settings.getString(GAME_UUID, mGame.uuid) ?: mGame.uuid
        try {
            mGame.updateGameState(Game.State.valueOf(settings.getString(GAME_STATE, Game.State.NORMAL.name)!!))
        } catch (e: Exception) {
            mGame.updateGameState(Game.State.NORMAL)
        }

        try {
            mGame.lastGameState = Game.State.valueOf(settings.getString(UNDO_GAME_STATE, Game.State.NORMAL.name)!!)
        } catch (e: Exception) {
            mGame.lastGameState = Game.State.NORMAL
        }

        mGame.updateUI()
    }

    private fun onLeftSwipe() {
        mGame.move(Game.DIRECTION_LEFT)
        Timber.d("Left Swipe")
    }

    private fun onRightSwipe() {
        mGame.move(Game.DIRECTION_RIGHT)
        Timber.d("Right Swipe")
    }

    private fun onDownSwipe() {
        mGame.move(Game.DIRECTION_DOWN)
        Timber.d("Down Swipe")
    }

    private fun onUpSwipe() {
        mGame.move(Game.DIRECTION_UP)
        Timber.d("Up Swipe")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                upX = event.x
                upY = event.y

                val deltaX = downX - upX
                val deltaY = downY - upY

                // swipe horizontal?
                if (abs(deltaX) > abs(deltaY)) {
                    if (abs(deltaX) > MIN_DISTANCE) {
                        // left or right
                        if (deltaX > 0) {
                            this.onLeftSwipe()
                            return true
                        }
                        if (deltaX < 0) {
                            this.onRightSwipe()
                            return true
                        }
                    } else {
                        return false // We don't consume the event
                    }
                } else {
                    if (abs(deltaY) > MIN_DISTANCE) {
                        // top or down
                        if (deltaY < 0) {
                            this.onDownSwipe()
                            return true
                        }
                        if (deltaY > 0) {
                            this.onUpSwipe()
                            return true
                        }
                    } else {
                        return false // We don't consume the event
                    }
                } // swipe vertical?

                return true
            }
        }
        return false
    }

    companion object {
        internal const val MIN_DISTANCE = 70

        private const val SCORE = "savegame.score"
        private const val UNDO_SCORE = "savegame.undoscore"
        private const val CAN_UNDO = "savegame.canundo"
        private const val UNDO_GRID = "savegame.undo"
        private const val GAME_STATE = "savegame.gamestate"
        private const val UNDO_GAME_STATE = "savegame.undogamestate"
        private const val GAME_UUID = "savegame.uuid"
    }
}
