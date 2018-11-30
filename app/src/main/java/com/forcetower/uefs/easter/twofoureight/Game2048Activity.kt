/*
 * Copyright (c) 2018.
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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import com.forcetower.uefs.R
import com.forcetower.uefs.easter.twofoureight.tools.KeyListener
import com.forcetower.uefs.feature.shared.UActivity
import timber.log.Timber

class Game2048Activity : UActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game2048)

        val window = window
        window.setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        )

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, Game2048Fragment(), "Game Fragment")
                .commit()
        }

        //        unlockAchievements(getString(R.string.achievement_you_found_me), mPlayGamesInstance);
        //        revealAchievement(getString(R.string.achievement_you_are_good_in_2048), mPlayGamesInstance);
        //        revealAchievement(getString(R.string.achievement_unes_2048_champion), mPlayGamesInstance);
        //        revealAchievement(getString(R.string.achievement_you_tried_2048), mPlayGamesInstance);
        //        revealAchievement(getString(R.string.achievement_practice_makes_perfect), mPlayGamesInstance);
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Timber.d("Action: $keyCode event: $event")
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            val current = supportFragmentManager.findFragmentByTag("Game Fragment")
            if (current is KeyListener) {
                return (current as KeyListener).onKeyDown(keyCode, event)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {

        fun startActivity(context: Context) {
            val intent = Intent(context, Game2048Activity::class.java)
            context.startActivity(intent)
        }
    }
}
