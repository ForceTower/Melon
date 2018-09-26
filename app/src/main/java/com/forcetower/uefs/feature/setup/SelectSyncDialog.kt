/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.feature.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.DialogSelectCourseBinding
import com.forcetower.uefs.databinding.DialogSelectSynchronizationBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import javax.inject.Inject

class SelectSyncDialog: RoundedDialog(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var binding: DialogSelectSynchronizationBinding
    private var data: Array<Frequency>? = null
    private var callback: FrequencySelectionCallback? = null

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogSelectSynchronizationBinding.inflate(inflater, container, false).also {
            binding = it
            it.btnCancel.setOnClickListener {_ -> dismiss()}
            it.btnOk.setOnClickListener {_ -> select() }
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val array = arrayListOf(
                Frequency(getString(R.string.at_every_time_minutes, 15), 15),
                Frequency(getString(R.string.at_every_time_minutes, 30), 30),
                Frequency(resources.getQuantityString(R.plurals.at_every_time_hours, 1), 60),
                Frequency(resources.getQuantityString(R.plurals.at_every_time_hours, 2), 120),
                Frequency(resources.getQuantityString(R.plurals.at_every_time_hours, 3), 180),
                Frequency(resources.getQuantityString(R.plurals.at_every_time_hours, 6), 360),
                Frequency(resources.getQuantityString(R.plurals.at_every_time_hours, 12), 720)
        )
        populateFrequency(array)
    }

    private fun populateFrequency(data: List<Frequency>) {
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

data class Frequency (
    val name: String,
    val value: Int
)

interface FrequencySelectionCallback {
    fun onSelected(frequency: Frequency)
}