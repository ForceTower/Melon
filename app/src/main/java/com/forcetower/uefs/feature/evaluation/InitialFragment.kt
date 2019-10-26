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

package com.forcetower.uefs.feature.evaluation

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.feature.shared.UFragment
import timber.log.Timber
import javax.inject.Inject

class InitialFragment : UFragment(), Injectable {
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var factory: UViewModelFactory
    private val viewModel: EvaluationViewModel by activityViewModels { factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return View(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val onboarding = preferences.getBoolean("evaluation_presentation_shown", false)
        viewModel.getToken().observe(viewLifecycleOwner, Observer {
            Timber.d("Token received: $it")
            if (it == null) {
                findNavController().navigate(R.id.action_initial_to_unesverse_required)
            } else if (!onboarding) {
                findNavController().navigate(R.id.action_initial_to_presentation)
            } else {
                findNavController().navigate(R.id.action_initial_to_home)
            }
        })
    }
}