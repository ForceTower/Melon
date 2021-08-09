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

package com.forcetower.uefs.feature.syncregistry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentSyncRegistryBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncRegistryFragment : UFragment() {
    private val viewModel: SyncRegistryViewModel by viewModels()
    private lateinit var binding: FragmentSyncRegistryBinding
    private lateinit var syncAdapter: SyncRegistryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentSyncRegistryBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            incToolbar.apply {
                textToolbarTitle.text = getString(R.string.label_sync_registry)
            }
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        syncAdapter = SyncRegistryAdapter()
        binding.recyclerRegistry.apply {
            adapter = syncAdapter
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }

        viewModel.registry.observe(viewLifecycleOwner, { syncAdapter.submitList(it) })
    }
}
