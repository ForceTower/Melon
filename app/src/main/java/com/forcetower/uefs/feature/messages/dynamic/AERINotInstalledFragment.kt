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

package com.forcetower.uefs.feature.messages.dynamic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.forcetower.uefs.databinding.FragmentMessagesAeriNotInstalledBinding
import com.forcetower.uefs.feature.messages.MessagesDFMViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AERINotInstalledFragment : UFragment() {
    private val dynamicViewModel: MessagesDFMViewModel by activityViewModels()

    private lateinit var binding: FragmentMessagesAeriNotInstalledBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentMessagesAeriNotInstalledBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            btnGetStarted.setOnClickListener { requestModuleInstall() }
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dynamicViewModel.downloadStatus.observe(
            viewLifecycleOwner,
            Observer {
                binding.progressInstall.run {
                    progress = it.first.toInt()
                    max = it.second.toInt()
                }
            }
        )
    }

    private fun requestModuleInstall() {
        dynamicViewModel.requestAERIInstall()
        dynamicViewModel.sessionStatusLive.observe(
            viewLifecycleOwner,
            Observer {
                onStatusUpdate(it)
            }
        )
    }

    private fun onStatusUpdate(@SplitInstallSessionStatus status: Int) {
        when (status) {
            SplitInstallSessionStatus.INSTALLING, SplitInstallSessionStatus.DOWNLOADING, SplitInstallSessionStatus.PENDING -> {
                binding.run {
                    groupGetStarted.visibility = View.INVISIBLE
                    groupInstalling.visibility = View.VISIBLE
                    binding.progressInstall.isIndeterminate = status != SplitInstallSessionStatus.DOWNLOADING
                }
            }
            SplitInstallSessionStatus.INSTALLED -> Unit
            else -> {
                binding.run {
                    groupGetStarted.visibility = View.VISIBLE
                    groupInstalling.visibility = View.INVISIBLE
                }
            }
        }
    }
}
