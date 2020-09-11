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

package dev.forcetower.conference

import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import com.forcetower.uefs.core.injection.dependencies.ConferenceModuleDependencies
import com.forcetower.uefs.feature.shared.UActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.EntryPointAccessors
import dev.forcetower.conference.core.injection.DaggerConferenceComponent
import dev.forcetower.conference.databinding.ActivityConferenceBinding

class ConferenceActivity : UActivity() {
    private lateinit var binding: ActivityConferenceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DaggerConferenceComponent.builder()
            .context(this)
            .dependencies(
                EntryPointAccessors.fromApplication(
                    applicationContext,
                    ConferenceModuleDependencies::class.java
                )
            )
            .build()
            .inject(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_conference)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun showSnack(string: String, duration: Int) {
        getSnackInstance(string, duration)?.show()
    }

    override fun getSnackInstance(string: String, duration: Int): Snackbar? {
        return Snackbar.make(binding.root, string, duration)
    }
}
