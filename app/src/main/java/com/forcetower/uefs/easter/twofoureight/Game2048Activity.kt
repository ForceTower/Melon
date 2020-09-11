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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.forcetower.uefs.R
import com.forcetower.uefs.core.vm.UserSessionViewModel
import com.forcetower.uefs.easter.twofoureight.tools.KeyListener
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class Game2048Activity : UGameActivity() {
    private val sessionViewModel: UserSessionViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game2048)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, Game2048Fragment(), "Game Fragment")
                .commit()
        }

        unlockAchievement(R.string.achievement_voc_me_achou)
        revealAchievement(R.string.achievement_voc__bom)
        revealAchievement(R.string.achievement_o_campeo_de_2048_no_unes)
        revealAchievement(R.string.achievement_eu_tentei)
        revealAchievement(R.string.achievement_a_prtica_leva__perfeio)
    }

    override fun showSnack(string: String, duration: Int) {
        getSnackInstance(string, duration).show()
    }

    override fun getSnackInstance(string: String, duration: Int): Snackbar {
        val snack = Snackbar.make(findViewById(R.id.container), string, duration)
        snack.config()
        return snack
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

    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionViewModel.onUserInteraction()
    }

    override fun onPause() {
        super.onPause()
        sessionViewModel.onUserInteraction()
    }

    override fun onResume() {
        super.onResume()
        sessionViewModel.onUserInteraction()
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionViewModel.onUserInteraction()
    }

    companion object {

        fun startActivity(context: Context) {
            val intent = Intent(context, Game2048Activity::class.java)
            context.startActivity(intent)
        }
    }
}
