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

package com.forcetower.uefs

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.forcetower.uefs.core.vm.Destination
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.LaunchViewModel
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.feature.home.HomeActivity
import com.forcetower.uefs.feature.login.LoginActivity
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.forcetower.uefs.feature.themeswitcher.ThemeOverlayUtils
import com.forcetower.uefs.feature.themeswitcher.ThemeSwitcherResourceProvider
import com.forcetower.uefs.service.NotificationCreator
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

/**
 * A atividade inicial do aplicativo, ela tem o papel de decidir qual tela mostrar
 * - Login -> caso o usuário não esteja conectado [Não existe usuário + senha no aplicativo]
 * - Home  -> caso o usuário esteja conectado
 */
class LauncherActivity : AppCompatActivity(), HasAndroidInjector {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Any>
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var remoteConfig: FirebaseRemoteConfig
    @Inject
    lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: LaunchViewModel = provideViewModel(factory)
        if (savedInstanceState != null) return
        createNewVersionNotification()

        viewModel.direction.observe(this, EventObserver {
            Timber.d("Once!")
            // Esta linha não é necessária já que o EventObserver é chamado apenas uma vez
            if (!viewModel.started) {
                when (it) {
                    Destination.LOGIN_ACTIVITY -> startActivity(Intent(this, LoginActivity::class.java))
                    Destination.HOME_ACTIVITY -> startActivity(Intent(this, HomeActivity::class.java))
                }
                viewModel.started = true
                finish()
            }
        })
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

    private fun onNextFeatureEaster() {
        val lucky = Random.nextInt(100)
        if (lucky < 90) {
            Timber.d("Unlucky!")
            return
        }
        val resourceProvider = ThemeSwitcherResourceProvider()

        val themesMap = arrayOf(
            intArrayOf(R.id.theme_feature_primary_color, getRandomTheme(resourceProvider.primaryColors)),
            intArrayOf(R.id.theme_feature_secondary_color, getRandomTheme(resourceProvider.secondaryColors))
        )

        for (i in themesMap.indices) {
            ThemeOverlayUtils.setThemeOverlay(themesMap[i][0], themesMap[i][1])
        }
    }

    private fun getRandomTheme(overlays: Int): Int {
        val themeAttrs = resources.obtainTypedArray(overlays)
        val resource = themeAttrs.getResourceId(Random.nextInt(themeAttrs.length()), 0)
        themeAttrs.recycle()
        return resource
    }

    override fun androidInjector() = fragmentInjector
}
