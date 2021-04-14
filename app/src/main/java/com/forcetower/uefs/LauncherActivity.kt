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
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.feature.home.HomeActivity
import com.forcetower.uefs.feature.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * A atividade inicial do aplicativo, ela tem o papel de decidir qual tela mostrar
 * - Login -> caso o usuário não esteja conectado [Não existe usuário + senha no aplicativo]
 * - Home  -> caso o usuário esteja conectado
 */
@AndroidEntryPoint
class LauncherActivity : AppCompatActivity() {
    private val viewModel by viewModels<LaunchViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return

        viewModel.checkNewAppVersion()
        viewModel.findStarterDirection()
        viewModel.direction.observe(
            this,
            EventObserver { destination ->
                when (destination) {
                    LaunchViewModel.Destination.LOGIN_ACTIVITY -> startActivity(Intent(this, LoginActivity::class.java))
                    LaunchViewModel.Destination.HOME_ACTIVITY -> startActivity(Intent(this, HomeActivity::class.java))
                }
                finish()
            }
        )
    }
}
