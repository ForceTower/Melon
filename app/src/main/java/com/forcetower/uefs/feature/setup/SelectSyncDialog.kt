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

package com.forcetower.uefs.feature.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.forcetower.uefs.core.model.service.SyncFrequency
import com.forcetower.uefs.databinding.DialogSelectSynchronizationBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectSyncDialog : RoundedDialog() {

    private val setupViewModel: SetupViewModel by activityViewModels()
    private lateinit var binding: DialogSelectSynchronizationBinding
    private var data: Array<SyncFrequency>? = null
    private var callback: FrequencySelectionCallback? = null

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogSelectSynchronizationBinding.inflate(inflater, container, false).also {
            binding = it
            it.btnCancel.setOnClickListener { dismiss() }
            it.btnOk.setOnClickListener { select() }
            populateFrequency(setupViewModel.syncFrequencies)
        }.root
    }

    private fun populateFrequency(data: List<SyncFrequency>) {
        this.data = data.toTypedArray()
        val strings = data.map { it.name }.toTypedArray()
        binding.pickerSync.minValue = 1
        binding.pickerSync.maxValue = strings.size
        binding.pickerSync.displayedValues = strings
    }

    private fun select() {
        val not = data ?: emptyArray()
        if (not.isNotEmpty()) {
            callback?.onSelected(not[binding.pickerSync.value - 1])
            dismiss()
        }
    }

    fun setCallback(callback: FrequencySelectionCallback) {
        this.callback = callback
    }
}

data class Frequency(
    val name: String,
    val value: Int
)

interface FrequencySelectionCallback {
    fun onSelected(frequency: SyncFrequency)
}
