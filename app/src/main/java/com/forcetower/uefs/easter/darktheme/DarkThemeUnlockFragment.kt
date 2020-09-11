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

package com.forcetower.uefs.easter.darktheme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentDarkThemeUnlockBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DarkThemeUnlockFragment : UFragment() {
    private val viewModel: DarkThemeViewModel by activityViewModels()
    private lateinit var binding: FragmentDarkThemeUnlockBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentDarkThemeUnlockBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val preconditionsAdapter = PreconditionsAdapter()
        binding.preconditionsRecycler.run {
            adapter = preconditionsAdapter
        }
        viewModel.preconditions.observe(viewLifecycleOwner, Observer { preconditionsAdapter.submitList(it) })

        binding.inviteToDark.setOnClickListener {
            findNavController().navigate(R.id.action_dark_event_to_dark_invite)
        }

        binding.inviteToDark.isEnabled = true
    }
}
