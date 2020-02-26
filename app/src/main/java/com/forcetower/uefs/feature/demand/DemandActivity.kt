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

package com.forcetower.uefs.feature.demand

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.forcetower.uefs.R
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.ActivityDemandBinding
import com.forcetower.uefs.feature.shared.NavigationFragment
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.extensions.config
import com.forcetower.uefs.feature.shared.extensions.inTransaction
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import timber.log.Timber
import javax.inject.Inject

class DemandActivity : UActivity(), HasAndroidInjector {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Any>
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var analytics: FirebaseAnalytics

    private lateinit var binding: ActivityDemandBinding
    private lateinit var currentFragment: NavigationFragment
    private lateinit var viewModel: DemandViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_demand)
        viewModel = provideViewModel(factory)

        if (savedInstanceState == null) {
            supportFragmentManager.inTransaction {
                val fragment = DemandOffersFragment()
                currentFragment = fragment
                add(R.id.fragment_container, fragment)
            }
            analytics.logEvent("demand_entered_screen", null)
        }

        viewModel.snackbarMessage.observe(this, EventObserver { showSnack(it) })
    }

    override fun androidInjector() = fragmentInjector

    override fun onBackPressed() {
        if (!::currentFragment.isInitialized || !currentFragment.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun showSnack(string: String, duration: Int) {
        Timber.d("Show snack called on activity")
        val snack = Snackbar.make(binding.snack, string, duration)
        snack.config(pxElevation = 8)
        snack.show()
    }

    companion object {
        fun startIntent(context: Context): Intent {
            return Intent(context, DemandActivity::class.java)
        }
    }
}