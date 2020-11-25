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

package com.forcetower.uefs

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.forcetower.uefs.core.vm.LaunchViewModel
import com.forcetower.uefs.service.NotificationCreator
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A atividade inicial do aplicativo, ela tem o papel de decidir qual tela mostrar
 * - Login -> caso o usuário não esteja conectado [Não existe usuário + senha no aplicativo]
 * - Home  -> caso o usuário esteja conectado
 */
@AndroidEntryPoint
class LauncherActivity : AppCompatActivity() {
    @Inject
    lateinit var remoteConfig: FirebaseRemoteConfig
    @Inject
    lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: LaunchViewModel by viewModels()
        if (savedInstanceState != null) return
        createNewVersionNotification()

        val intent = Intent(Intent.ACTION_VIEW).setClassName(
            this,
            "dev.forcetower.map.MapActivity"
        )

        startActivity(intent)
        finish()
        return

//        viewModel.direction.observe(
//            this,
//            EventObserver {
//                Timber.d("Once!")
//                // Esta linha não é necessária já que o EventObserver é chamado apenas uma vez
//                if (!viewModel.started) {
//                    when (it) {
//                        Destination.LOGIN_ACTIVITY -> startActivity(Intent(this, LoginActivity::class.java))
//                        Destination.HOME_ACTIVITY -> startActivity(Intent(this, HomeActivity::class.java))
//                    }
//                    viewModel.started = true
//                    finish()
//                }
//            }
//        )
    }

    private fun createNewVersionNotification() {
        val currentVersion = remoteConfig.getLong("version_current")
        val notified = preferences.getBoolean("version_ntf_key_$currentVersion", false)
        if (currentVersion > BuildConfig.VERSION_CODE && !notified) {
            val notes = remoteConfig.getString("version_notes")
            val version = remoteConfig.getString("version_name")
            val title = getString(R.string.new_version_ntf_title_format, version)
            NotificationCreator.showSimpleNotification(this, title, notes)
            preferences.edit().putBoolean("version_ntf_key_$currentVersion", true).apply()
        }
    }
}
