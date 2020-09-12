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

package com.forcetower.uefs.feature.bigtray

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.architecture.service.bigtray.BigTrayService
import com.forcetower.uefs.core.model.bigtray.BigTrayData
import com.forcetower.uefs.core.model.bigtray.isOpen
import com.forcetower.uefs.core.model.bigtray.percentage
import com.forcetower.uefs.databinding.FragmentBigTrayBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.formatDateTime
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BigTrayFragment : UFragment() {
    private val viewModel: BigTrayViewModel by viewModels()
    private lateinit var binding: FragmentBigTrayBinding
    private var hasData = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentBigTrayBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.incToolbar.apply {
            textToolbarTitle.text = getString(R.string.label_big_tray)
            appBar.elevation = 0f
        }
        binding.btnNotification.setOnClickListener { BigTrayService.startService(requireContext()) }
    }

    override fun onStart() {
        super.onStart()
        viewModel.requesting = true
        viewModel.data().observe(this, Observer { onDataSnapshot(it) })
    }

    private fun onDataSnapshot(data: BigTrayData?) {
        data ?: return
        binding.groupLoading.visibility = GONE
        binding.textUpdate.text = getString(R.string.ru_last_update, data.time.formatDateTime())

        if (data.isOpen() && !data.error) {
            binding.groupOpen.visibility = VISIBLE
            binding.groupClosed.visibility = GONE
            binding.groupFailed.visibility = GONE
            val percent = data.percentage()
            binding.progressAmount.setProgressWithAnimation(percent)
            binding.textAmount.text = data.quota
            hasData = true
        } else if (!data.error) {
            binding.groupOpen.visibility = GONE
            binding.groupClosed.visibility = VISIBLE
            binding.groupFailed.visibility = GONE
            hasData = true
        } else if (!hasData) {
            binding.groupOpen.visibility = GONE
            binding.groupClosed.visibility = GONE
            binding.groupFailed.visibility = VISIBLE
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.requesting = false
    }
}
