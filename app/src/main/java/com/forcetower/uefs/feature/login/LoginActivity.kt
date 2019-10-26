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

package com.forcetower.uefs.feature.login

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.core.vm.CourseViewModel
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.ActivityLoginBinding
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import timber.log.Timber
import javax.inject.Inject

class LoginActivity : UActivity(), HasAndroidInjector {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Any>
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var preferences: SharedPreferences

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        val coursesViewModel = provideViewModel<CourseViewModel>(factory)
        coursesViewModel.courses.observe(this, Observer {
            Timber.d("Courses Status: ${it.status}")
        })

        if (savedInstanceState == null) {
            onActivityStart()
        }
    }

    private fun onActivityStart() {
        val enabled = preferences.getBoolean("ach_night_mode_enabled", false)
        if (enabled) return

        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> Unit
            Configuration.UI_MODE_NIGHT_YES -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                recreate()
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                recreate()
            }
        }
    }

    override fun navigateUpTo(upIntent: Intent?): Boolean = findNavController(R.id.login_nav_host).navigateUp()

    override fun showSnack(string: String, long: Boolean) {
        val snack = Snackbar.make(binding.root, string, if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT)
        snack.config()
        snack.show()
    }

    override fun androidInjector() = fragmentInjector
}
