/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.forcetower.uefs.R
import com.forcetower.uefs.easter.twofoureight.tools.KeyListener
import com.forcetower.uefs.feature.shared.UGameActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.google.android.material.snackbar.Snackbar
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import timber.log.Timber
import javax.inject.Inject

class Game2048Activity : UGameActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector() = fragmentInjector

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

        unlockAchievement(R.string.achievement_voc_me_achou)
        revealAchievement(R.string.achievement_voc__bom)
        revealAchievement(R.string.achievement_o_campeo_de_2048_no_unes)
        revealAchievement(R.string.achievement_eu_tentei)
        revealAchievement(R.string.achievement_a_prtica_leva__perfeio)
    }

    override fun showSnack(string: String, long: Boolean) {
        val snack = Snackbar.make(findViewById(R.id.container), string, if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT)
        snack.config()
        snack.show()
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
