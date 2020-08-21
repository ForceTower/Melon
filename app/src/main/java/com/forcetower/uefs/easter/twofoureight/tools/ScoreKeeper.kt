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

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.widget.TextView
import com.forcetower.uefs.easter.twofoureight.view.Game

class ScoreKeeper(context: Context) : ContextWrapper(context), Game.ScoreListener {
    private var mScoreDisplay: TextView? = null
    private var mHighScoreDisplay: TextView? = null
    private val mPreferences: SharedPreferences?
    private var mScoreListener: Game.ScoreListener? = null
    private var mScore: Long = 0
    private var mHighScore: Long = 0

    init {
        mPreferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    }

    fun setViews(score: TextView, highScore: TextView) {
        mScoreDisplay = score
        mHighScoreDisplay = highScore
        reset()
    }

    private fun reset() {
        mHighScore = loadHighScore()
        if (mHighScoreDisplay != null) {
            val highScore = "" + mHighScore
            mHighScoreDisplay!!.text = highScore
        }
        mScore = 0
        if (mScoreDisplay != null) {
            val score = "" + mScore
            mScoreDisplay!!.text = score
        }
    }

    fun setScoreListener(scoreListener: Game.ScoreListener?) {
        this.mScoreListener = scoreListener
    }

    private fun loadHighScore(): Long {
        return mPreferences?.getLong(HIGH_SCORE, 0) ?: -1
    }

    private fun saveHighScore(highScore: Long) {
        val editor = mPreferences!!.edit()
        editor.putLong(HIGH_SCORE, highScore)
        editor.apply()
    }

    private fun setScore(score: Long) {
        mScore = score
        if (mScoreDisplay != null) {
            val scoreT = "" + mScore
            mScoreDisplay!!.text = scoreT
        }
        if (mScore > mHighScore) {
            mHighScore = mScore
            if (mHighScoreDisplay != null) {
                val highScore = "" + mHighScore
                mHighScoreDisplay!!.text = highScore
            }
            saveHighScore(mHighScore)
        }
    }

    override fun onNewScore(score: Long) {
        setScore(score)
        mScoreListener?.onNewScore(score)
    }

    companion object {
        const val HIGH_SCORE = "score.highscore"
        const val PREFERENCES = "score"
    }
}
