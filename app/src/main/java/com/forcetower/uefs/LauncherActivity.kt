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

package com.forcetower.uefs

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.forcetower.uefs.core.vm.Destination
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.LaunchViewModel
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.feature.home.HomeActivity
import com.forcetower.uefs.feature.login.LoginActivity
import com.forcetower.uefs.feature.obsolete.ObsoleteActivity
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.forcetower.uefs.service.NotificationCreator
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import timber.log.Timber
import javax.inject.Inject

/**
 * A atividade inicial do aplicativo, ela tem o papel de decidir qual tela mostrar
 * - Login -> caso o usuário não esteja conectado [Não existe usuário + senha no aplicativo]
 * - Home  -> caso o usuário esteja conectado
 */
class LauncherActivity : AppCompatActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
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

        val disabledCode = remoteConfig.getLong("version_disable")
        if (disabledCode > BuildConfig.VERSION_CODE) {
            Timber.d("This version has been disabled")
            startActivity(Intent(this, ObsoleteActivity::class.java))
            finish()
        } else {
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

    override fun supportFragmentInjector() = fragmentInjector
}
