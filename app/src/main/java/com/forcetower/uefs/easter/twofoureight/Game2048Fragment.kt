/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.easter.twofoureight

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
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
import com.forcetower.uefs.R
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.databinding.GameFragment2048Binding
import com.forcetower.uefs.easter.darktheme.DarkThemeRepository
import com.forcetower.uefs.easter.twofoureight.tools.InputListener
import com.forcetower.uefs.easter.twofoureight.tools.KeyListener
import com.forcetower.uefs.easter.twofoureight.tools.ScoreKeeper
import com.forcetower.uefs.easter.twofoureight.view.Game
import com.forcetower.uefs.easter.twofoureight.view.Tile
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.UGameActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by João Paulo on 02/06/2018.
 */
class Game2048Fragment : UFragment(), KeyListener, Game.GameStateListener, View.OnTouchListener, Injectable, RewardedVideoAdListener {
    override fun onRewardedVideoAdLeftApplication() = Unit
    override fun onRewardedVideoAdLoaded() = Unit
    override fun onRewardedVideoAdOpened() = Unit
    override fun onRewardedVideoCompleted() = Unit
    override fun onRewardedVideoStarted() = Unit

    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var darkRepository: DarkThemeRepository
    @Inject
    lateinit var remoteConfig: FirebaseRemoteConfig

    private var downX: Float = 0.toFloat()
    private var downY: Float = 0.toFloat()
    private var upX: Float = 0.toFloat()
    private var upY: Float = 0.toFloat()
    private var backs: Int = 0

    private lateinit var mGame: Game
    private lateinit var binding: GameFragment2048Binding
    private var activity: UGameActivity? = null
    private lateinit var interstitial: InterstitialAd
    private lateinit var rewardedVideoAd: RewardedVideoAd

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as? UGameActivity
        activity ?: Timber.e("Adventure Fragment must be attached to a UGameActivity for it to work")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.game_fragment_2048, container, false)
        binding.gamePad.setOnTouchListener(this)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val mScoreKeeper = ScoreKeeper(requireActivity())

        mScoreKeeper.setViews(binding.tvScore, binding.tvHighscore)
        mScoreKeeper.setScoreListener(object : Game.ScoreListener {
            override fun onNewScore(score: Long) {
                if (score >= 50000) {
                    unlockDarkTheme()
                }
            }
        })

        mGame = Game()
        mGame.setup(binding.gameview)
        mGame.setScoreListener(mScoreKeeper)
        mGame.setGameStateListener(this)
        mGame.newGame()
        val input = InputListener()
        input.setView(binding.gameview)
        input.setGame(mGame)

        val admobEnabled = remoteConfig.getBoolean("admob_enabled")

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
            if (mGame.score > 100000 && backs <= 0 && admobEnabled) {
                promptBuyBacks()
            } else {
                mGame.revertUndoState()
                backs -= 1
                if (mGame.gameState === Game.State.ENDLESS || mGame.gameState === Game.State.ENLESS_WON) {
                    binding.tvTitle.text = HtmlCompat.fromHtml("&infin;", FROM_HTML_MODE_LEGACY)
                } else {
                    binding.tvTitle.text = HtmlCompat.fromHtml("2048", FROM_HTML_MODE_LEGACY)
                }
            }
        }

        binding.ibReset.setOnLongClickListener {
            Toast.makeText(activity, getString(R.string.start_new_game), Toast.LENGTH_SHORT).show()
            true
        }

        prepareInterstitialAds()
        prepareRewardedAds()
    }

    private fun prepareRewardedAds() {
        rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(requireContext())
        rewardedVideoAd.rewardedVideoAdListener = this
        loadRewardedAd()
    }

    private fun loadRewardedAd() {
        val request = AdRequest.Builder()
                .addTestDevice(Constants.ADMOB_TEST_ID)
                .build()
        rewardedVideoAd.loadAd(getString(R.string.admob_rewarded_2048_back_10_times), request)
    }

    private fun prepareInterstitialAds() {
        interstitial = InterstitialAd(requireContext())
        interstitial.adUnitId = getString(R.string.admob_interstitial_2048_lost_game)
        val request = AdRequest.Builder()
                .addTestDevice(Constants.ADMOB_TEST_ID)
                .build()
        interstitial.loadAd(request)
    }

    private fun promptBuyBacks() {
        if (rewardedVideoAd.isLoaded) {
            rewardedVideoAd.show()
        }
        onRewarded()
    }

    private fun onRewarded() {
        backs += 10
        mGame.revertUndoState()
        backs -= 1
    }

    private fun unlockDarkTheme() {
        val enabled = preferences.getBoolean("ach_night_mode_enabled", false)
        if (!enabled) {
            preferences.edit().putBoolean("ach_night_mode_enabled", true).apply()
            darkRepository.getPreconditions()
        }
    }

    override fun onPause() {
        if (::rewardedVideoAd.isInitialized)
            rewardedVideoAd.pause(requireContext())
        save()
        super.onPause()
    }

    override fun onResume() {
        if (::rewardedVideoAd.isInitialized)
            rewardedVideoAd.resume(requireContext())
        load()
        if (mGame.gameState === Game.State.ENDLESS || mGame.gameState === Game.State.ENLESS_WON) {
            binding.tvTitle.text = HtmlCompat.fromHtml("&infin;", FROM_HTML_MODE_LEGACY)
        }
        super.onResume()
    }

    override fun onDestroy() {
        if (::rewardedVideoAd.isInitialized)
            rewardedVideoAd.destroy(requireContext())
        super.onDestroy()
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
        val admobEnabled = remoteConfig.getBoolean("admob_enabled")
        if (state == Game.State.WON || state == Game.State.ENLESS_WON) {
            binding.tvEndgameOverlay.visibility = VISIBLE
            binding.tvEndgameOverlay.setText(R.string.you_win)

            activity?.unlockAchievement(R.string.achievement_voc__bom)
            activity?.incrementAchievementProgress(R.string.achievement_o_campeo_de_2048_no_unes, 1)
        } else if (state == Game.State.LOST) {
            binding.tvEndgameOverlay.visibility = VISIBLE
            binding.tvEndgameOverlay.setText(R.string.game_over)
            activity?.unlockAchievement(R.string.achievement_eu_tentei)
            activity?.incrementAchievementProgress(R.string.achievement_a_prtica_leva__perfeio, 1)
            if (interstitial.isLoaded && admobEnabled) {
                interstitial.show()
            }
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
            for (yy in 0 until field[0].size) {
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
        for (xx in 0 until mGame.gameGrid!!.grid.size) {
            for (yy in 0 until mGame.gameGrid!!.grid[0].size) {
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
                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    if (Math.abs(deltaX) > MIN_DISTANCE) {
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
                    if (Math.abs(deltaY) > MIN_DISTANCE) {
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

    override fun onRewarded(item: RewardItem?) {
        item ?: return
        if (item.type === "back") {
            backs += item.amount
            showSnack(getString(R.string.rewarded_with_back_2048))
        }
    }

    override fun onRewardedVideoAdClosed() {
        loadRewardedAd()
    }

    override fun onRewardedVideoAdFailedToLoad(reason: Int) {
        showSnack(getString(R.string.rewarded_failed))
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
