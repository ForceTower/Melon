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

package com.forcetower.uefs.feature.universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.core.vm.UnesverseViewModel
import com.forcetower.uefs.databinding.FragmentUniverseRequiredBinding
import com.forcetower.uefs.feature.information.InformationDialog
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UnesverseRequiredFragment : UFragment() {
    private lateinit var binding: FragmentUniverseRequiredBinding
    private val viewModel: UnesverseViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentUniverseRequiredBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            btnConnect.setOnClickListener { connect() }
            btnQuestion.setOnClickListener { onQuestion() }
        }.root
    }

    private fun onQuestion() {
        val dialog = InformationDialog()
        dialog.title = getString(R.string.unesverse_what_is_title)
        dialog.description = getString(R.string.unesverse_what_is_description)
        dialog.show(childFragmentManager, "what_is_unesverse")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.connecting = viewModel.isLoggingIn
        binding.lifecycleOwner = this
        viewModel.loggingIn.observe(viewLifecycleOwner, Observer { Unit })
        viewModel.loginMessenger.observe(
            viewLifecycleOwner,
            EventObserver {
                val message = getString(it)
                showSnack(message)
            }
        )
        viewModel.access.observe(
            viewLifecycleOwner,
            {
                if (it != null) {
                    findNavController().navigate(R.id.action_unesverse_required_to_presentation)
                }
            }
        )
    }

    private fun connect() {
        viewModel.login()
    }
}
