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

package com.forcetower.uefs.feature.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.storage.repository.SettingsRepository
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    context: Context
) : ViewModel() {
    private val splitInstallManager = SplitInstallManagerFactory.create(context)
    private var done: Boolean = false

    val isDarkModeEnabled: LiveData<Boolean>
        get() = repository.hasDarkModeEnabled()

    fun getAllTheGrades() {
        if (done) return
        done = true
        repository.requestAllGradesAndCalculateScore()
    }

    fun uninstallModuleIfExists(name: String) {
        if (splitInstallManager.installedModules.contains(name)) {
            splitInstallManager.deferredUninstall(listOf(name)).addOnCompleteListener {}
        }
    }
}
